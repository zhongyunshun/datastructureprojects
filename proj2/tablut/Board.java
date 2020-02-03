package tablut;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;


import static tablut.Piece.EMPTY;
import static tablut.Piece.KING;
import static tablut.Square.BOARD_SIZE;
import static tablut.Square.SQUARE_LIST;
import static tablut.Square.sq;


/**
 * The state of a Tablut Game.
 *
 * @author Yunshun Zhong
 */
class Board {

    /**
     * The number of squares on a side of the board.
     */
    static final int SIZE = 9;

    /**
     * The throne (or castle) square and its four surrounding squares.
     */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /**
     * Initial positions of attackers.
     */
    static final Square[] INITIAL_ATTACKERS = {
            sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
            sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
            sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
            sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /**
     * Initial positions of defenders of the king.
     */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
        sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /**
     * Initializes a game board with SIZE squares on a side in the
     * initial position.
     */
    Board() {
        init();
    }

    /**
     * Initializes a copy of MODEL.
     */
    Board(Board model) {
        copy(model);
    }

    /**
     * Copies MODEL into me.
     */
    void copy(Board model) {
        if (model == this) {
            return;
        }

        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                put(model.get(col, row), sq(col, row));
            }
        }

        this._moveCount = model.moveCount();
        this._repeated = model.repeatedPosition();
        this._turn = model.turn();
        this._winner = model.winner();
        this._limit = model._limit;

        this.boardStateStack.clear();
        for (String state : model.boardStateStack) {
            this.boardStateStack.push(state);
        }
    }

    /**
     * Clears the board to the initial position.
     */
    void init() {
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                put(Piece.EMPTY, sq(col, row));
            }
        }

        put(Piece.KING, THRONE);

        for (Square square : INITIAL_DEFENDERS) {
            put(Piece.WHITE, square);
        }

        for (Square square : INITIAL_ATTACKERS) {
            put(Piece.BLACK, square);
        }

        this._moveCount = 0;
        this._winner = null;
        this._turn = Piece.BLACK;
        this._repeated = false;
        this._limit = 0;

        this.boardStateStack.clear();
        this.boardStateStack.push(encodedBoard());
    }

    /**
     * Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     *
     * @param n the limit number of move.
     */
    void setMoveLimit(int n) {
        assert 2 * n > moveCount() && n > 0;
        this._limit = n;
    }

    /**
     * Return a Piece representing whose move it is (WHITE or BLACK).
     */
    Piece turn() {
        return _turn;
    }

    /**
     * Return the winner in the current position, or null if there is no winner
     * yet.
     */
    Piece winner() {
        return _winner;
    }

    /**
     * Returns true iff this is a win due to a repeated position.
     */
    boolean repeatedPosition() {
        return _repeated;
    }

    /**
     * Record current position and set winner() next mover if the current
     * position is a repeat.
     */
    private void checkRepeated() {
        if (boardStateStack.isEmpty() || boardStateStack.size() < 5) {
            _repeated = false;
            return;
        }

        String curState = _turn.opponent().toString()
                + encodedBoard().substring(1);

        int curIndex = 0;

        for (String historyState : boardStateStack) {
            if (curIndex == boardStateStack.size() - 4) {
                if (curState.equals(historyState)) {
                    _winner = _turn.opponent();
                    _repeated = true;
                }
                break;
            }
            curIndex++;
        }
    }

    /**
     * Return the number of moves since the initial position that have not been
     * undone.
     */
    int moveCount() {
        return _moveCount;
    }

    /**
     * Return the number of limit.
     */
    int limit() {
        return _limit;
    }

    /**
     * Return location of the king.
     */
    Square kingPosition() {
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                Piece curPiece = get(col, row);
                if (Piece.KING == curPiece) {
                    return sq(col, row);
                }
            }
        }
        return null;
    }

    /**
     * Return the contents the square at S.
     */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /**
     * Return the contents of the square at (COL, ROW), where
     * 0 <= COL, ROW <= 9.
     */
    final Piece get(int col, int row) {
        return checkerBoard[col][row];
    }

    /**
     * Return the contents of the square at COL ROW.
     */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /**
     * Set square S to P.
     */
    final void put(Piece p, Square s) {
        checkerBoard[s.col()][s.row()] = p;
    }

    /**
     * Set square S to P and record for undoing.
     */
    final void revPut(Piece p, Square s) {
        checkerBoard[s.col()][s.row()] = p;
        boardStateStack.push(encodedBoard());
    }

    /**
     * Set square COL ROW to P.
     */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /**
     * Return true iff FROM - TO is an unblocked rook move on the current
     * board.  For this to be true, FROM-TO must be a rook move and the
     * squares along it, other than FROM, must be empty.
     */
    boolean isUnblockedMove(Square from, Square to) {
        int scol = from.col();
        int ecol = to.col();
        int srow = from.row();
        int erow = to.row();
        if (from.col() < to.col()) {
            scol = from.col() + 1;
            ecol = to.col();
        } else if (from.col() > to.col()) {
            scol = to.col();
            ecol = from.col() - 1;
        }
        if (from.row() < to.row()) {
            srow = from.row() + 1;
            erow = to.row();
        } else if (from.row() > to.row()) {
            srow = to.row();
            erow = from.row() - 1;
        }
        for (int col = scol; col <= ecol; col++) {
            for (int row = srow; row <= erow; row++) {
                if (Piece.EMPTY != get(col, row)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return true iff FROM is a valid starting square for a move.
     */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /**
     * Return true iff FROM-TO is a valid move.
     */
    boolean isLegal(Square from, Square to) {
        if (!from.isRookMove(to)) {
            return false;
        }

        if (!isLegal(from)) {
            return false;
        }

        if (THRONE == to && Piece.KING != get(from)) {
            return false;
        }

        return isUnblockedMove(from, to);
    }

    /**
     * Return true iff MOVE is a legal move in the current
     * position.
     */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /**
     * Move FROM-TO, assuming this is a legal move.
     */
    void makeMove(Square from, Square to) {

        Piece piece = get(from);

        put(piece, to);
        put(Piece.EMPTY, from);
        _moveCount++;

        if (Piece.KING == get(to) && to.isEdge()) {
            _winner = Piece.WHITE;
            return;
        }

        checkRepeated();
        if (_repeated && _winner != null) {
            return;
        }

        List<Square> rookSquares = rookSquare(to);
        rookSquares.forEach(square -> capture(to, square));
        if (_winner != null) {
            return;
        }

        HashSet<Square> blacks = pieceLocations(Piece.BLACK);
        if (blacks == null || blacks.isEmpty()) {
            _winner = Piece.WHITE;
            return;
        }

        if (!hasMove(piece.opponent())) {
            _winner = _turn;
        } else if (checkMoveCount()) {
            _winner = piece.opponent();
        } else {
            _turn = piece.opponent();
            boardStateStack.push(encodedBoard());
        }
    }

    /**
     * Move according to MOVE, assuming it is a legal move.
     */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /**
     * Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     * SQ0 and the necessary conditions are satisfied.
     */
    private void capture(Square sq0, Square sq2) {
        Piece piece0 = get(sq0);
        Piece piece2 = get(sq2);

        if (piece0.side() != piece2.side()) {
            if (sq2 != THRONE || piece2 == KING) {
                return;
            }
        }

        Square s1 = sq0.between(sq2);
        Piece piece1 = get(s1);

        if (piece1 == Piece.EMPTY || piece1.side() == piece0.side()) {
            return;
        }

        if (Piece.KING == piece1) {
            boolean blackWin = false;
            if (THRONE == s1) {
                if (get(NTHRONE) == Piece.BLACK && get(ETHRONE) == Piece.BLACK
                        && get(STHRONE) == Piece.BLACK
                        && get(WTHRONE) == Piece.BLACK) {
                    blackWin = true;
                }
            } else if (NTHRONE == s1 || ETHRONE == s1 || STHRONE == s1
                    || WTHRONE == s1) {
                int blackCount = 0;
                for (int dir = 0; dir <= 3; dir++) {
                    Square square = s1.rookMove(dir, 1);
                    if (square != null && get(square) == Piece.BLACK) {
                        blackCount++;
                    }
                }
                if (blackCount == 3) {
                    blackWin = true;
                }
            } else {
                blackWin = true;
            }
            if (blackWin) {
                _winner = Piece.BLACK;
                put(Piece.EMPTY, s1);
                _repeated = true;
            }
        } else {
            put(Piece.EMPTY, s1);
        }

    }

    /**
     * Undo one move.  Has no effect on the initial board.
     */
    void undo() {
        if (_moveCount > 0) {
            undoPosition();
            _moveCount--;
            _winner = null;
        }
    }

    /**
     * Remove record of current position in the set of positions encountered,
     * unless it is a repeated position or we are at the first move.
     */
    private void undoPosition() {
        if (boardStateStack.isEmpty()) {
            return;
        }
        boardStateStack.pop();
        String previous = boardStateStack.peek();

        char[] states = previous.toCharArray();
        assert states.length == SQUARE_LIST.size() + 1;

        _turn = Piece.getPieceBySymbol("" + states[0]);

        for (int index = 1; index < states.length; index++) {
            Square square = sq(index - 1);
            Piece piece = Piece.getPieceBySymbol("" + states[index]);
            put(piece, square);
        }
        _repeated = false;
    }

    /**
     * Clear the undo stack and board-position counts. Does not modify the
     * current position or win status.
     */
    void clearUndo() {
        boardStateStack.clear();
        this._moveCount = 0;
    }

    /**
     * Return a new mutable list of all legal moves on the current board for
     * SIDE (ignoring whose turn it is at the moment).
     */
    List<Move> legalMoves(Piece side) {
        HashSet<Square> squares = pieceLocations(side);
        List<Move> moves = new ArrayList<>();

        for (Square square : squares) {
            for (int dir = 0; dir <= 3; dir++) {
                int step = 1;
                while (true) {
                    Square temp = square.rookMove(dir, step++);
                    if (temp == null || get(temp) != Piece.EMPTY) {
                        break;
                    }
                    moves.add(Move.mv(square, temp));
                }
            }
        }

        return moves;
    }

    /**
     * Return true iff SIDE has a legal move.
     */
    boolean hasMove(Piece side) {
        List<Move> moves = legalMoves(side);
        return !moves.isEmpty();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Return a text representation of this Board.  If COORDINATES, then row
     * and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /**
     * Return the locations of all pieces on SIDE.
     */
    public HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;

        Piece targetSize = side.side();
        HashSet<Square> set = new HashSet<>();

        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                if (checkerBoard[col][row].side() == targetSize) {
                    set.add(sq(col, row));
                }
            }
        }
        return set;
    }

    /**
     * Return the contents of _board in the order of SQUARE_LIST as a sequence
     * of characters: the toString values of the current turn and Pieces.
     */
    String encodedBoard() {
        char[] result = new char[SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /**
     * Piece whose turn it is (WHITE or BLACK).
     */
    private Piece _turn;
    /**
     * Cached value of winner on this board, or EMPTY if it has not been
     * computed.
     */
    private Piece _winner;
    /**
     * Number of (still undone) moves since initial position.
     */
    private int _moveCount;
    /**
     * True when current board is a repeated position (ending the game).
     */
    private boolean _repeated;

    /**
     * Limit N, the max_count for each side, the default value is 0,
     * which means unlimited.
     */
    private int _limit;

    /**
     * Define the board.
     */
    private Piece[][] checkerBoard = new Piece[BOARD_SIZE][BOARD_SIZE];

    /**
     * the undo stack.
     */
    private Stack<String> boardStateStack = new Stack<>();

    /**
     * get the squares that is separated from the square
     * by one position(horizontal or vertical).
     *
     * @param square means the center square.
     *
     * @return the list of square which is near the square.
     */
    List<Square> rookSquare(Square square) {
        int col = square.col();
        int row = square.row();
        int step = 2;

        List<Square> rookSquares = new ArrayList<>();
        if (col < BOARD_SIZE - 2) {
            rookSquares.add(square.rookMove(1, step));
        }
        if (col > 1) {
            rookSquares.add(square.rookMove(3, step));
        }
        if (row < BOARD_SIZE - 2) {
            rookSquares.add(square.rookMove(0, step));
        }
        if (row > 1) {
            rookSquares.add(square.rookMove(2, step));
        }
        return rookSquares;
    }

    /**
     * Check if the game is over.
     *
     * @return game over return true, else false.
     */
    boolean checkGameOver() {
        Square king = kingPosition();
        return king == null || king.isEdge();
    }

    /**
     * Check iff the moveCount > limit.
     *
     * @return moveCount is over return true, else false.
     */
    boolean checkMoveCount() {
        int sideCount = moveCount() / 2;
        sideCount = moveCount() % 2 == 0 ? sideCount : (sideCount + 1);
        if (_limit > 0 && sideCount >= _limit) {
            return true;
        }
        return false;
    }

}
