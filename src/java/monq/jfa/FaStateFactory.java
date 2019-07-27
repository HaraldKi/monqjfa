package monq.jfa;

public abstract class FaStateFactory<S extends FaState<S>> {
  public abstract S create();
  public abstract S create(FaAction a);
  
  public static final FaStateFactory<NfaState> forNfa = 
      new FaStateFactory<>() {
        @Override public NfaState create() {
          return new NfaState();
        }

        @Override
        public NfaState create(FaAction a) {
          return new NfaState(a);
        }
  };

  public static final FaStateFactory<DfaState> forDfa = 
      new FaStateFactory<>() {
        @Override public DfaState create() {
          return new DfaState();
        }

        @Override
        public DfaState create(FaAction a) {
          return new DfaState(a);
        }
  };
  
}
