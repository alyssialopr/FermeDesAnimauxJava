package laFerme;

public class Vache implements Animal {
    private static int CPT = 1;

    int id;
    String nom;
    String race;
    String couleur;
    String enclos;

    public Vache(String nom, String race, String couleur, String enclos){
        this.id = CPT++;
        this.nom = nom;
        this.race = race;
        this.couleur = couleur;
        this.enclos = enclos;
        //this.etat = EtatAnimal.LIBRE;
    }

}
