const SERVER_URL = 'http://localhost:8080/api';

// CONTOARE LIVE PENTRU MONITORIZAREA STATISTICILOR DASHBOARD-ULUI
let totalInregistrati = 0;
let cazuriCritice = 0;
let consultatiRecent = 0;

// Tabloul în care memorăm istoricul pacienților consultați în sesiunea curentă
let istoricConsultatii = [];

function updateDashboardUI() {
    document.getElementById('stat-total').innerText = totalInregistrati;
    document.getElementById('stat-critici').innerText = cazuriCritice;
    document.getElementById('stat-consultati').innerText = consultatiRecent;
}

// LOGICĂ NAVIGARE PAGINI (TAB-URI)
function switchTab(tabId) {
    document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    document.getElementById(tabId).classList.add('active');
    const clickedBtn = Array.from(document.querySelectorAll('.nav-btn')).find(btn =>
        btn.getAttribute('onclick').includes(tabId)
    );
    if (clickedBtn) clickedBtn.classList.add('active');

    // Dacă medicul trece pe tab-ul cabinetului, încărcăm starea live a Heap-ului
    if (tabId === 'tab-cabinet') {
        incarcaPacientiActivi();
    }
}

// 1. ÎNREGISTRARE PACIENT (POST)
document.getElementById('addForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const id = document.getElementById('pId').value;
    const nume = document.getElementById('pName').value;
    const diagnostic = document.getElementById('pDiag').value;
    const simptome = document.getElementById('pSymptoms').value;
    const prioritate = document.getElementById('pPriority').value;

    const bodyData = `id=${encodeURIComponent(id)}&nume=${encodeURIComponent(nume)}&diagnostic=${encodeURIComponent(diagnostic)}&simptome=${encodeURIComponent(simptome)}&prioritate=${encodeURIComponent(prioritate)}`;

    fetch(`${SERVER_URL}/addPacient`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: bodyData
    })
    .then(res => res.json())
    .then(data => {
        alert("Sistem: Fișa clinică a fost validată și salvată în Min-Heap și B-Tree.");
        if(data.status === 'success' || data.status === 'ok') {
            totalInregistrati++;
            if (prioritate === "1") cazuriCritice++;
            updateDashboardUI();

            document.getElementById('addForm').reset();
        }
    })
    .catch(err => alert('Eroare: Lipsă conexiune backend.'));
});

// 2. PREUARE PACIENT URGENT (CABINET) ȘI INJECTARE ÎN ISTORIC
function preiaUrmatorul() {
    fetch(`${SERVER_URL}/getUrmatorul`)
    .then(res => res.json())
    .then(data => {
        const box = document.getElementById('triageResult');
        box.classList.remove('display-placeholder');

        if(data.status === 'empty') {
            box.innerHTML = `<span>Nu există nicio fișă medicală în așteptare pentru consult.</span>`;
            box.classList.add('display-placeholder');
        } else {
            if(totalInregistrati > 0) totalInregistrati--;
            if((data.prioritate.includes("Roșu") || data.prioritate.includes("VERY_URGENT")) && cazuriCritice > 0) cazuriCritice--;
            consultatiRecent++;
            updateDashboardUI();

            box.innerHTML = `<div style="text-align:left;">
                                <h4 style="color:var(--accent-neon); margin: 0 0 10px 0; font-size:16px;">Pacient Repartizat Curent:</h4>
                                <strong>Nume și Prenume:</strong> ${data.nume} <br>
                                <strong>Număr Fișă:</strong> #${data.id} <br>
                                <strong>Diagnostic Principal:</strong> ${data.diagnostic} <br>
                                <strong>Nivel Triaj:</strong> <span style="color:var(--danger-neon); font-weight:bold;">${data.prioritate}</span>
                             </div>`;

            const acum = new Date();
            const oraFormatata = acum.getHours().toString().padStart(2, '0') + ':' + acum.getMinutes().toString().padStart(2, '0');

            istoricConsultatii.unshift({
                ora: oraFormatata,
                id: data.id,
                nume: data.nume,
                diagnostic: data.diagnostic,
                prioritate: data.prioritate
            });

            actualizeazaTabelIstoric();
            incarcaPacientiActivi();
        }
    })
    .catch(err => alert('Eroare la decontarea pacientului din server!'));
}

// ====================================================
// MONITORIZARE LIVE ȘI ȘTERGERE SIMULTANĂ
// ====================================================

function incarcaPacientiActivi() {
    fetch(`${SERVER_URL}/getPacientiActivi`)
    .then(res => res.json())
    .then(data => {
        const placeholder = document.getElementById('activeQueuePlaceholder');
        const container = document.getElementById('activeQueueContainer');
        const tbody = document.getElementById('activeQueueTableBody');

        if (!data || data.length === 0) {
            placeholder.style.display = "block";
            container.style.display = "none";
            return;
        }

        placeholder.style.display = "none";
        container.style.display = "block";
        tbody.innerHTML = "";

        data.forEach(p => {
            let culoareText = "#fff";
            if(p.prioritate.includes("Roșu") || p.prioritate.includes("CRITIC")) culoareText = "var(--danger-neon)";
            else if(p.prioritate.includes("Galben")) culoareText = "#fbbf24";

            const rand = document.createElement('tr');
            rand.style.borderBottom = "1px solid rgba(255,255,255,0.05)";

            // Butonul elimină complet pacientul din ambele structuri
            rand.innerHTML = `
                <td style="padding: 10px 8px; font-weight: bold; color: var(--primary-neon);">#${p.id}</td>
                <td style="padding: 10px 8px;">${p.nume}</td>
                <td style="padding: 10px 8px; font-weight: 600; color: ${culoareText}">${p.prioritate}</td>
                <td style="padding: 10px 8px; text-align: center;">
                    <button onclick="eliminaComplet('${p.nume}', '${p.prioritate}')" style="background: var(--danger-neon); color: #0f172a; border: none; padding: 5px 12px; border-radius: 5px; font-weight: 700; cursor: pointer; font-size: 11px; transition: 0.2s;">
                        ✕ Elimină Dosar (Șterge din B-Tree & Heap)
                    </button>
                </td>
            `;
            tbody.appendChild(rand);
        });
    })
    .catch(err => console.log('Eroare la încărcarea structurii live.'));
}

// Funcție asincronă unificată pentru eliminarea din B-Tree și curățarea automată a Heap-ului
function eliminaComplet(numePacient, prioritateEticheta) {
    if(!confirm(`Sunteți sigur că doriți să eliminați definitiv pacientul "${numePacient}"? Această acțiune va șterge în cascadă istoricul din B-Tree și fisa din coada Min-Heap.`)) return;

    fetch(`${SERVER_URL}/stergeBTree`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `nume=${encodeURIComponent(numePacient)}`
    })
    .then(res => res.json())
    .then(data => {
        if(data.status === 'success') {
            alert(data.message);

            // Recalculăm statisticile live de pe interfață
            if(totalInregistrati > 0) totalInregistrati--;
            if((prioritateEticheta.includes("Roșu") || prioritateEticheta.includes("CRITIC")) && cazuriCritice > 0) {
                cazuriCritice--;
            }
            updateDashboardUI();

            // Reîmprospătăm imediat stiva pentru a vedea eliminarea live în tabel
            incarcaPacientiActivi();
        } else {
            alert(`Eroare backend: ${data.message}`);
        }
    })
    .catch(err => alert('Eroare la trimiterea request-ului de ștergere integrată către server.'));
}

function actualizeazaTabelIstoric() {
    const placeholder = document.getElementById('historyPlaceholder');
    const container = document.getElementById('historyTableContainer');
    const tbody = document.getElementById('historyTableBody');

    if (istoricConsultatii.length === 0) {
        placeholder.style.display = "block";
        container.style.display = "none";
        return;
    }

    placeholder.style.display = "none";
    container.style.display = "block";
    tbody.innerHTML = "";

    istoricConsultatii.forEach(p => {
        let culoareTriaj = "#94a3b8";
        if (p.prioritate.includes("Roșu") || p.prioritate.includes("CRITIC")) culoareTriaj = "var(--danger-neon)";
        else if (p.prioritate.includes("Galben")) culoareTriaj = "#fbbf24";
        else if (p.prioritate.includes("Verde")) culoareTriaj = "var(--accent-neon)";
        else if (p.prioritate.includes("Albastru")) culoareTriaj = "var(--primary-neon)";

        const rand = document.createElement('tr');
        rand.style.borderBottom = "1px solid rgba(255,255,255,0.05)";

        rand.innerHTML = `
            <td style="padding: 10px 8px; color: var(--text-muted);">${p.ora}</td>
            <td style="padding: 10px 8px; font-weight: bold; color: var(--primary-neon);">#${p.id}</td>
            <td style="padding: 10px 8px;">${p.nume}</td>
            <td style="padding: 10px 8px; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${p.diagnostic}</td>
            <td style="padding: 10px 8px; color: ${culoareTriaj}; font-weight: 600;">${p.prioritate}</td>
        `;
        tbody.appendChild(rand);
    });
}

// 3. CĂUTARE DUPĂ NUME EXACT (ARHIVĂ)
function cautaInBTree() {
    const nume = document.getElementById('searchNameInput').value.trim();
    if(!nume) return alert('Introduceți un nume!');

    fetch(`${SERVER_URL}/cautaNume?nume=${encodeURIComponent(nume)}`)
    .then(res => res.json())
    .then(data => {
        const box = document.getElementById('bTreeResult');
        if(data.status === 'gasit') {
            box.innerHTML = `<div style="text-align:left; color:#fff;">
                                <strong style="color:var(--primary-neon);">Dosar Identificat în Registru:</strong><br>
                                <strong>ID Fișă:</strong> #${data.id}<br>
                                <strong>Pacient:</strong> ${data.nume}<br>
                                <strong>Diagnostic înregistrat:</strong> ${data.diagnostic}<br>
                                <strong>Simptomatologie detaliată:</strong> ${data.simptome}
                             </div>`;
        } else {
            box.innerHTML = `<span style="color:var(--danger-neon);">Niciun istoric clinic identificat pentru numele specificat.</span>`;
        }
    })
    .catch(err => alert('Eroare la interogarea sistemului!'));
}

// 4. FILTRARE DUPĂ SIMPTOME (ARHIVĂ)
function cautaCuKMP() {
    const text = document.getElementById('searchKMPInput').value.trim();
    if(!text) return alert('Introduceți un termen de filtrare!');

    fetch(`${SERVER_URL}/cautaSimptom?simptom=${encodeURIComponent(text)}`)
    .then(res => res.json())
    .then(data => {
        const box = document.getElementById('kmpResult');

        let htmlContinut = `<strong style="color:var(--accent-neon);">Corelații depistate pentru filtrul: "${text}"</strong><br>`;
        htmlContinut += `Total dosare asociate: <strong>${data.rezultate}</strong><br><br>`;

        if (data.rezultate > 0) {
            data.pacienti.forEach((pacient) => {
                htmlContinut += `<div class="patient-entry" style="text-align:left; color:#fff;">`;
                htmlContinut += `<strong>Pacient:</strong> ${pacient.nume} <span class="badge-ui">Fișă #${pacient.id}</span><br>`;
                htmlContinut += `<strong>Triaj:</strong> ${pacient.prioritate}<br>`;
                htmlContinut += `<strong>Diagnostic:</strong> ${pacient.diagnostic}<br>`
                htmlContinut += `<strong>Simptome active:</strong> ${pacient.simptome}<br>`;
                htmlContinut += `</div>`;
            });
        } else {
            htmlContinut += `<span style="color:var(--danger-neon);">Nu s-au găsit dosare medicale care să conțină acest istoric clinic.</span>`;
        }

        box.innerHTML = htmlContinut;
    })
    .catch(err => alert('Eroare la scanarea datelor!'));
}