package dkvs;

public class Ballot {
    public static final String splitter = "_";
    private int leaderId;
    private int ballotNum;

    public Ballot(int ballotNum, int leaderId) {
        this.ballotNum = ballotNum;
        this.leaderId = leaderId;
    }

    public Ballot() {
        this.ballotNum = -1;
        this.leaderId = 0;
    }

    public static Ballot parse(String s) {
        String[] parts = s.split(splitter);
        return new Ballot(Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]));
    }

    public boolean less(Ballot other) {
        int result = new Integer(getBallotNum()).compareTo(other.getBallotNum());
        if (result == 0)
            result = new Integer(getLeaderId()).compareTo(other.getLeaderId());
        return result < 0;
    }

    public boolean equals(Ballot other) {
        return (getBallotNum() == other.getBallotNum()) && (getLeaderId() == other.getLeaderId());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Ballot && (((Ballot) obj).getLeaderId() == getLeaderId()) && (((Ballot) obj).getBallotNum() == getBallotNum());
    }

    @Override
    public int hashCode() {
        return getBallotNum() * 3 + getLeaderId();
    }

    @Override
    public String toString() {
        return getBallotNum() + splitter + getLeaderId();
    }

    public int getBallotNum() {
        return ballotNum;
    }

    public int getLeaderId() {
        return leaderId;
    }
}
