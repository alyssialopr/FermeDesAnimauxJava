package laFerme;

public class Vache implements Animal {
    private static int CPT = 1;

    int id;
    String nom;
    String race;
    String couleur;
    EtatAnimal etat = EtatAnimal.LIBRE; // par defaut
    String enclos;

    public Vache(String nom, String race, String couleur, String enclos){
        this.id = CPT++;
        this.nom = nom;
        this.race = race;
        this.couleur = couleur;
        this.enclos = enclos;
        this.etat = EtatAnimal.LIBRE;
    }

    public void acheter(){
        setEtat(EtatAnimal.LIBRE);
    }

    public void vendre(){
        setEtat(EtatAnimal.VENDU);
    }

    public void mourir(){
        setEtat(EtatAnimal.MORT);
    }

    public void naitre(){
        setEtat(EtatAnimal.LIBRE);
    }

    public void nourrir(){
        System.out.println(nom + " a été nourrie.");
    }

    public void soigner(){
        System.out.println(nom + " a été soignée.");
    }

    public void allerEnBalade(){
        System.out.println(nom + " part en balade.");
    }

    public EtatAnimal getEtat() {
        return etat;
    }

    public void setEtat(EtatAnimal etat){
        this.etat = etat;
    }

    public void setEnclos(String enclos) {
        this.enclos = enclos;
    }
}
