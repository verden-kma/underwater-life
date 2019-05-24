package tempBackend;

public class Tuple <T,E> {
    public T getV1() {
        return v1;
    }

    public E getV2() {
        return v2;
    }

    private T v1;
    private E v2;

    public Tuple(T val_1, E val_2){
        v1 = val_1;
        v2 = val_2;
    }

}
