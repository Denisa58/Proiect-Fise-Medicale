/**
 * Implementare algoritm KMP pentru filtrarea rapida de text
 */
public class KMP {

    /**
     * Construieste tabela de prefixe
     * Analizeaza sablonul pentru a gasi lungimile celor mai lungi prefixe care sunt si sufixe
     * @param pattern
     * @return
     */
    private static int[] computePrefixFunction(String pattern)
    {
        int m = pattern.length();
        int[] pi = new int [m];

        pi[0] = 0;
        int k = 0; //lungimea prefixului potrivit anterior

        //parcurgem sablonul
        for(int q = 1; q < m; q++)
        {
            //cat timp nu se potrivesc facem salt inapoi conform vectorului pi
            while(k>0 && pattern.charAt(k) != pattern.charAt(q))
            {
                k = pi[k-1];
            }

            //daca am gasit potrivire creste lungimea prefixului curent
            if(pattern.charAt(k) == pattern.charAt(q))
            {
                k = k + 1;
            }
            pi[q] = k;
        }
        return pi;
    }

    /**
     * Executa cautarea sablonului in textul dat
     * @param text
     * @param pattern
     * @return true daca sablonul e gaist in text si fals in caz contrar
     */
    public static boolean search(String text, String pattern){
        if(pattern == null || pattern.isEmpty()) return true;
        if(text == null || text.isEmpty()) return false;

        text = text.toLowerCase();
        pattern = pattern.toLowerCase();

        int n = text.length();
        int m = pattern.length();

        if(m > n) return false;

        //generam tabela de prefixe pentru cuvantul cautat
        int[] pi = computePrefixFunction(pattern);
        int q = 0;

        for(int i = 0; i < n; i++)
        {
            //cand caracterele nu se mai potrivesc KMP sare peste portiunile verificate deja
            while(q > 0 && pattern.charAt(q) != text.charAt(i)){
                q = pi[q-1];
            }
            //daca caracterul curent coincide avansam
            if(pattern.charAt(q) == text.charAt(i)){
                q = q + 1;
            }

            if(q == m){
                return true; //cuvantul a fost identificat ci succes in text
            }
        }
        return false;
    }

}
