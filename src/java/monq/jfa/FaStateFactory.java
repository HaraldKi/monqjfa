package monq.jfa;

public abstract class FaStateFactory<S extends FaState<S>> {
  public abstract S create();
  public abstract S create(FaAction a);
  
  public static final FaStateFactory<AbstractFaState> forNfa = 
      new FaStateFactory<AbstractFaState>() {
        @Override public AbstractFaState create() {
          return new AbstractFaState();
        }

        @Override
        public AbstractFaState create(FaAction a) {
          return new AbstractFaState(a);
        }
  };

  public static final FaStateFactory<DfaState> forDfa = 
      new FaStateFactory<DfaState>() {
        @Override public DfaState create() {
          return new DfaState();
        }

        @Override
        public DfaState create(FaAction a) {
          return new DfaState(a);
        }
  };
  
}
