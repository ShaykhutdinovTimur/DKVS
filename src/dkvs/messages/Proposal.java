package dkvs.messages;

import dkvs.Ballot;

import java.util.Arrays;

public class Proposal {
    private Ballot ballotNum;
    private int slot;
    private Command command;

    public Proposal(Ballot ballotNum, int slot, Command command) {
        this.ballotNum = ballotNum;
        this.slot = slot;
        this.command = command;
    }

    public static Proposal parse(String[] parts) {
        return new Proposal(Ballot.parse(parts[0]),
                Integer.parseInt(parts[1]),
                Command.parse(Arrays.copyOfRange(parts, 2, parts.length)));
    }

    @Override
    public String toString() {
        return ballotNum + " " + slot + " " + command;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Proposal) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }

    public Ballot getBallotNum() {
        return ballotNum;
    }

    public int getSlot() {
        return slot;
    }

    public Command getCommand() {
        return command;
    }
}