import javafx.beans.binding.NumberBinding;
import javafx.beans.property.SimpleLongProperty;

public class Launcher {

    public static void main(String[] args) {
        SimpleLongProperty a = new SimpleLongProperty(Math.round(100*0.1));
        //a = newa.getValue()*5;
        NumberBinding nb = a.add(5);
        NumberBinding nbs = nb.add(8);
        System.out.println(nbs.getValue());

    }
}
