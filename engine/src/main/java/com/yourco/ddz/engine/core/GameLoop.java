package com.yourco.ddz.engine.core;
import java.util.*;
public final class GameLoop {
  private final Rules rules;
  private final GameState state;
  private final Queue<GameAction> inbox=new ArrayDeque<>();
  private boolean scored=false;

  public GameLoop(Rules rules, GameState initialState){
      this.rules=Objects.requireNonNull(rules); this.state=Objects.requireNonNull(initialState);
  }

  public GameState state(){
      return state;
  }

  public void submit(GameAction a){
      inbox.add(a);
  }

  public void tick(){
      while(!inbox.isEmpty() && !rules.isTerminal(state)){
          var a=inbox.poll();
          rules.apply(state,a);
          state.addAction(a);
      }
      if(rules.isTerminal(state) && !scored){
          rules.score(state);
          scored=true;
      }
  }
}