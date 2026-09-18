package laFerme.model;

import java.util.ArrayList;
import java.util.List;

public class Eleveur {
    private static int CPT = 1;

    int id;
    String prenom;
    List<Animal> animaux;

    public Eleveur(String prenom) {
        this.id = CPT++;
        this.prenom = prenom;
        this.animaux = new ArrayList<>();
    }

    public Animal acheter(Animal animal){
        animal.acheter();
        this.animaux.add(animal);
        return animal;
    }

    public Animal vendre(Animal animal){
        animal.vendre();
        this.animaux.remove(animal);
        return animal;
    }


    public Animal allerEnBalade(Animal animal){
        animal.allerEnBalade();
        this.animaux.remove(animal);
        return animal;
    }


    public void nourrir(Animal animal){
        if(animaux.contains(animal)){
            animal.nourrir();
        }
        else{
            throw new IllegalArgumentException("Cet animal ne vous appartient pas");
        }
    }

    public void soigner(Animal animal){
        if(animaux.contains(animal)){
            animal.soigner();
        }
        else{
            throw new IllegalArgumentException("Cet animal ne vous appartient pas");
        }
    }

    public void promener(Animal animal){
        if(animaux.contains(animal)){
            animal.allerEnBalade();
        }
        else{
            throw new IllegalArgumentException("Cet animal ne vous appartient pas");
        }
    }

}
