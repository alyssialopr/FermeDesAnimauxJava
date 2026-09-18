package laFerme.model;

public interface Animal {

    void nourrir();
    void acheter();
    void allerEnBalade();
    void vendre();
    void soigner();
    EtatAnimal getEtat();
    void setEnclos(String enclos);
}
