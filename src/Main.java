import laFerme.Eleveur;
import laFerme.Poule;
import laFerme.Vache;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        var Emily = new Vache("emily", "Highland","marron","1");
        var Marguerite = new Vache("marguerite", "Charolaise", "blanche", "2");

        var Nugget = new Poule("nugget", "suisse", "orange", "3" );
        var Plume = new Poule("plume", "francais", "blanche","3");

        var Alyssia = new Eleveur("alyssia");
        var Killian = new Eleveur("killian");

        Killian.acheter(Nugget);
        Killian.acheter(Plume);

        Alyssia.acheter(Emily);
        Alyssia.acheter(Marguerite);

        System.out.println(Nugget.getEtat());

    }
}