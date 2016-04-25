package edu.umn.kylepete.player;

import java.util.List;

import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public interface SubAgent {
    public List<ScoredMove> scoreValidMoves() throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException;
}
