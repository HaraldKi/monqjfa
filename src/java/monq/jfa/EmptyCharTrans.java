package monq.jfa;

public class EmptyCharTrans<T> implements CharTrans<T> {
  private static final EmptyCharTrans<?> INSTANCE =
      new EmptyCharTrans<>();
  
  public static <T> EmptyCharTrans<T> instance() {
    @SuppressWarnings("unchecked")
    EmptyCharTrans<T> tmp = (EmptyCharTrans<T>)INSTANCE;
    return tmp;
  }
  
  @Override
  public T get(char ch) {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public T getAt(int pos) {
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public char getFirstAt(int pos) {
    throw new ArrayIndexOutOfBoundsException();
  }

  @Override
  public char getLastAt(int pos) {
    throw new ArrayIndexOutOfBoundsException();
  }

}
