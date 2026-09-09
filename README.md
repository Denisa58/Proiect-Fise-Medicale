# Sistem de Gestionare a Fișelor Medicale

Aplicație web pentru managementul pacienților și triaj medical. Proiectul eficientizează timpul de așteptare în saloanele de urgență prin prioritizarea automată a cazurilor și gestionarea arhivei de fișe medicale.

## Funcționalități

- **Coada de Așteptare (Triaj):** Pacienții sunt ordonați automat în funcție de gravitate.
- **Arhivă Medicală:** Căutare alfabetică rapidă după nume și filtrare după simptome.
- **Sincronizare în Timp Real:** Eliminarea unui pacient din arhivă îl șterge automat și din coada de așteptare.

## Arhitectură și Structuri de Date

- **Min-Heap (Vectorial):** Gestionează coada de triaj (rădăcina conține mereu pacientul cu urgența cea mai mare).
- **B-Tree:** Stochează arhiva generală a spitalului, indexată alfabetic după numele pacientului.
- **Algoritmul KMP:** Motor de căutare rapid pentru filtrarea fișelor după simptome/diagnostice.

## Complexitate

| Operație | Complexitate Timp | Descriere |
| :--- | :--- | :--- |
| **Adăugare Pacient** | O(log n) | Inserare în Heap și B-Tree |
| **Preluare Următorul Pacient** | O(log n) | Extragere din Min-Heap |
| **Căutare după Nume** | O(log_t n) | Căutare în B-Tree |
| **Filtrare Simptome** | O(n + m) | Scanare text cu algoritmul KMP |
| **Ștergere Fișă** | O(n) | Ștergere din B-Tree și căutare ID în Heap |

## Tehnologii Utilizate

- **Backend:** Java
- **Frontend:** HTML, CSS, JavaScript
- **Algoritmi:** Min-Heap, B-Tree, KMP
