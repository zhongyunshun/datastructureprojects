package tablut;

import java.util.HashSet;
import java.util.List;


import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.random;
import static tablut.Square.BOARD_SIZE;
import static tablut.Square.sq;

/**
 * A Player that automatically generates moves.
 *
 * @author Yunshun Zhong
 */
class AI extends Player {

    /**
     * A position-score magnitude indicating a win (for white if positive,
     * black if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /**
     * A position-score magnitude indicating a forced win in a subsequent
     * move.  This differs from WINNING_VALUE to avoid putting off wins.
     */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;

    /**
     * A new AI with no piece or controller (intended to produce
     * a template).
     */
    AI() {
        this(null, null);
    }

    /**
     * A new AI playing PIECE under control of CONTROLLER.
     */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        Board b = board();
        if (b.winner() != null || b.turn() != myPiece()) {
            _controller.reportError("misplaced move");
            return null;
        } else {
            Move move = findMove();
            if (move == null || !board().isLegal(move)) {
                _controller.reportError("Invalid move. " + "Please try again.");
                return null;
            }
            return move.toString();
        }
    }

    @Override
    boolean isManual() {
        return false;
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        int alpha = -INFTY;
        int beta = INFTY;
        boolean saveMove = true;
        int sense = 1;
        if (b.turn() == Piece.BLACK) {
            sense = -sense;
        }
        int depth = maxDepth(board());
        findMove(b, depth, saveMove, sense, alpha, beta);
        return _lastFoundMove;
    }

    /**
     * The move found by the last call to one of the ...FindMove methods
     * below.
     */
    private Move _lastFoundMove;

    /**
     * The weight of the captured.
     */
    private final int capturedWeight = 100000;

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove iff SAVEMOVE. The move
     * should have maximal value or have value > BETA if SENSE==1,
     * and minimal value or value < ALPHA if SENSE==-1. Searches up to
     * DEPTH levels.  Searching at level 0 simply returns a static estimate
     * of the board value and does not set _lastMoveFound.
     */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (depth == 1 || board.checkGameOver()) {
            return sense == 1 ? simpleFindMax(board, alpha, beta)
                    : simpleFindMin(board, alpha, beta);
        }
        int bestSoFar = sense == 1 ? -INFTY : INFTY;
        List<Move> moves = board.legalMoves(board.turn());
        for (Move move : moves) {
            Board nextBoard = new Board(board);
            nextBoard.makeMove(move);
            int nextSense = -sense;
            int nextScore = findMove(nextBoard, depth - 1, false,
                    nextSense, alpha, beta);
            if (sense == 1) {
                if (nextScore >= bestSoFar) {
                    bestSoFar = nextScore;
                    alpha = max(alpha, nextScore);
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            } else {
                if (nextScore <= bestSoFar) {
                    bestSoFar = nextScore;
                    beta = min(beta, nextScore);
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                    if (beta <= alpha) {
                        break;
                    }
                }
            }
        }
        if (_lastFoundMove == null) {
            _lastFoundMove = moves.get(0);
        }
        return bestSoFar;
    }

    /**
     * Return a heuristically determined maximum search depth
     * based on characteristics of BOARD.
     */
    private static int maxDepth(Board board) {
        Square king = board.kingPosition();
        if (king == null || king.isEdge()) {
            return 1;
        }

        int moveCount = board.moveCount();
        int limitCount = board.limit();
        int restCount = limitCount * 2 - moveCount;
        if (restCount <= 2 && restCount > 0) {
            return 1;
        } else if (restCount <= 4 && restCount > 0) {
            return 2;
        }
        if (whiteWillWin(board, king) || blackWillWin(board, king)) {
            return 2;
        }
        if (restCount <= 6 && restCount > 0) {
            return 3;
        }
        return 4;
    }

    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        Square king = board.kingPosition();
        if (king == null) {
            return -WINNING_VALUE;
        } else if (king.isEdge()) {
            return WINNING_VALUE;
        }

        HashSet<Square> blackPieces = board.pieceLocations(Piece.BLACK);
        if (blackPieces == null || blackPieces.isEmpty()
                || blackPieces.size() < 4) {
            return WINNING_VALUE;
        }

        if (whiteWillWin(board, king)) {
            return WILL_WIN_VALUE;
        } else if (blackWillWin(board, king)) {
            return -WILL_WIN_VALUE;
        }

        int capturedScore = captured(board) * capturedWeight;
        if (board.turn() == Piece.BLACK) {
            capturedScore = -capturedScore;
        }

        HashSet<Square> whitePieces = board.pieceLocations(Piece.WHITE);

        int squareScore = ((12 * whitePieces.size() - 9 * blackPieces.size()
                + 10) / (whitePieces.size() + blackPieces.size() + 1) * 1000);

        int nearKingBlack = nearKingBlack(board, king) * 10;

        return capturedScore - nearKingBlack + squareScore
                + (int) (random() * 1000);
    }

    /**
     * The last layer which is max, find the max score.
     *
     * @param board the board which will be checked.
     * @param alpha the alpha of the current layer.
     * @param beta  the beta of the current layer.
     * @return the score of the current layer.
     */
    private int simpleFindMax(Board board, int alpha, int beta) {
        Square square = board.kingPosition();
        if (square == null) {
            return -WINNING_VALUE;
        } else if (square.isEdge()) {
            return WINNING_VALUE;
        }
        int bestSoFar = -WINNING_VALUE;
        List<Move> moves = board.legalMoves(board.turn());
        for (Move move : moves) {
            Board nextBoard = new Board(board);
            nextBoard.makeMove(move);
            int nextScore = staticScore(nextBoard);
            if (nextScore >= bestSoFar) {
                bestSoFar = nextScore;
                alpha = max(alpha, nextScore);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        return bestSoFar;
    }

    /**
     * The last layer which is min, find the min score.
     *
     * @param board the board which will be checked.
     * @param alpha the alpha of the current layer.
     * @param beta  the beta of the current layer.
     * @return the score of the current layer.
     */
    private int simpleFindMin(Board board, int alpha, int beta) {
        Square square = board.kingPosition();
        if (square == null) {
            return -WINNING_VALUE;
        } else if (square.isEdge()) {
            return WINNING_VALUE;
        }
        int bestSoFar = WINNING_VALUE;
        List<Move> moves = board.legalMoves(board.turn());
        for (Move move : moves) {
            Board nextBoard = new Board(board);
            nextBoard.makeMove(move);
            int nextScore = staticScore(nextBoard);
            if (nextScore <= bestSoFar) {
                bestSoFar = nextScore;
                beta = min(beta, nextScore);
                if (beta <= alpha) {
                    break;
                }
            }
        }
        return bestSoFar;
    }

    /**
     * check the board state,
     * if the White will win in next step, return true, else return false.
     *
     * @param board  the board which will be checked.
     * @param king   the square of the King.
     * @return if the White will win in next step, return true, else false.
     */
    private static boolean whiteWillWin(Board board, Square king) {
        if (king == null || king.isEdge()) {
            return true;
        }

        int col = king.col();
        int row = king.row();

        for (int i = col + 1; i < BOARD_SIZE; i++) {
            if (!(board.get(sq(i, row)) == Piece.EMPTY)) {
                break;
            } else if (i == BOARD_SIZE - 1) {
                return true;
            }
        }

        for (int i = 0; i < col; i++) {
            if (!(board.get(sq(i, row)) == Piece.EMPTY)) {
                break;
            } else if (i == col - 1) {
                return true;
            }
        }

        for (int i = row + 1; i < BOARD_SIZE; i++) {
            if (!(board.get(sq(col, i)) == Piece.EMPTY)) {
                break;
            } else if (i == BOARD_SIZE - 1) {
                return true;
            }
        }

        for (int i = 0; i < row; i++) {
            if (!(board.get(sq(col, i)) == Piece.EMPTY)) {
                break;
            } else if (i == row - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * check the board state,
     * if the Black will win in next step, return true, else return false.
     *
     * @param board the board which will be checked.
     * @param king  the square of the King.
     * @return if the Black will win in next step, return true, else false.
     */
    private static boolean blackWillWin(Board board, Square king) {

        int blackSize = 0;
        for (int dir = 0; dir <= 3; dir++) {
            Square nearKing = king.rookMove(dir, 1);
            if (board.get(nearKing) == Piece.BLACK) {
                blackSize++;
            }
        }
        if (blackSize >= 3) {
            return true;
        }
        return false;
    }

    /**
     * Count the number of pieces that have been captured,
     * according to the current chess player.
     *
     * @param board the board which will be checked.
     * @return the number of Piece which has been captured.
     */
    private int captured(Board board) {
        Piece turn = board.turn();
        String captured = turn.toString() + "-" + turn.toString();

        int capturedCount = 0;

        for (int i = 0; i < BOARD_SIZE; i++) {
            StringBuilder colStr = new StringBuilder();
            StringBuilder rowStr = new StringBuilder();
            for (int j = 0; j < BOARD_SIZE; j++) {
                colStr.append(board.get(sq(j, i)));
                rowStr.append(board.get(sq(i, j)));
            }
            int idx = 0;
            while (colStr.indexOf(captured, idx) >= 0) {
                idx += 3;
                capturedCount++;
            }
            idx = 0;
            while (rowStr.indexOf(captured, idx) >= 0) {
                idx += 3;
                capturedCount++;
            }
        }

        return capturedCount;
    }

    /**
     * Count the number of pieces which is near the King.
     *
     * @param board  the board which will be checked.
     * @param king  the Square of King
     * @return the number of Black Piece which is near King
     */
    private int nearKingBlack(Board board, Square king) {
        int nearKingBlack = 0;
        int col = king.col();
        int row = king.row();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                Square square = sq(col + i, row + j);
                if (board.get(square) == Piece.BLACK) {
                    nearKingBlack += 1;
                    if (i == 0 || j == 0) {
                        nearKingBlack += 2;
                    }
                }
            }
        }
        return nearKingBlack;
    }
}
