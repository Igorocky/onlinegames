package org.igor.onlinegames.model;

public interface GameState {
    boolean isWaitingForPlayersToJoin();
    boolean isInProgress();
    String gameType();
    String gameDisplayType();
    String getTitle();
    boolean hasPasscode();
    String getShortDescription();
    boolean isOwner(UserSessionData userData);
    boolean mayBeRemoved();
    void preDestroy();
}
