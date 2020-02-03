package tablut;

import java.util.List;
import org.junit.Test;
import ucb.junit.textui;


import static org.junit.Assert.assertTrue;

/**
 * The suite of all JUnit tests for the enigma package.
 *
 * @author Yunshun Zhong
 */
public class UnitTest {

    /**
     * Run the JUnit tests in this package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * check the init board.
     */
    @Test
    public void initBoardTest() {
        Board board = new Board();
        String encodeBoard = "B---BBB-------B--------W----B---W---BBBWWKWWBBB"
                + "---W---B----W--------B-------BBB---";
        assertTrue("Init the Board Fail!",
                encodeBoard.equals(board.encodedBoard()));
    }

    /**
     * check the move.
     */
    @Test
    public void legalMoveTest() {
        Board board = new Board();
        assertTrue("This is not legal move!",
                board.isLegal(Move.mv("i6-g")));
        assertTrue("This is not legal move!",
                !board.isLegal(Move.mv("i6-e")));
    }

    /**
     * check the rook square.
     */
    @Test
    public void rookSquareTest() {
        Board board = new Board();
        List<Square> squares1 = board.rookSquare(Square.sq("i", "6"));
        assertTrue("This is not the correct square",
                squares1.size() == 3);
        assertTrue("This is not the correct square",
                squares1.contains(Square.sq("g", "6")));
        assertTrue("This is not the correct square",
                squares1.contains(Square.sq("i", "8")));
        assertTrue("This is not the correct square",
                squares1.contains(Square.sq("i", "4")));


        List<Square> squares2 = board.rookSquare(Square.sq("g", "6"));
        assertTrue("This is not the correct square",
                squares2.size() == 4);
        assertTrue("This is not the correct square",
                squares2.contains(Square.sq("i", "6")));
        assertTrue("This is not the correct square",
                squares2.contains(Square.sq("e", "6")));
        assertTrue("This is not the correct square",
                squares2.contains(Square.sq("g", "8")));
        assertTrue("This is not the correct square",
                squares2.contains(Square.sq("g", "4")));
    }

    /**
     * check the legal moves.
     */
    @Test
    public void legalMovesTest() {
        Board board = new Board();
        List<Move> moves = board.legalMoves(Piece.WHITE);
        assertTrue("This is not the all moves",
                56 == moves.size());
    }

    /**
     * A dummy test as a placeholder for real ones.
     */
    @Test
    public void dummyTest() {
    }

}


