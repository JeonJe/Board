package board;


import utils.DBUtils;
import utils.StringUtils;

import java.sql.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BoardDAO {

    /**
     * 게시글 내용 저장
     *
     * @param board
     * @return
     * @throws Exception
     */
    public int save(Board board) throws Exception {
        int boardId = 0;

        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            LocalDateTime currentTime = LocalDateTime.now();
            board.setCreatedAt(currentTime);
            board.setModifiedAt(currentTime);

            String sql = "INSERT INTO board (category_id, writer, password, title, content," + " created_at, modified_at,visit_count) VALUES (?, ?, ?, ?, ?, ?, ?,?)";

            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, board.getCategoryId());
            pstmt.setString(2, board.getWriter());
            pstmt.setString(3, board.getPassword());
            pstmt.setString(4, board.getTitle());
            pstmt.setString(5, board.getContent());
            pstmt.setString(6, board.getCreatedAt());
            pstmt.setString(7, board.getModifiedAt());
            pstmt.setInt(8, board.getVisitCount());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    boardId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("게시글 생성에 실패했습니다.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
        return boardId;
    }

    /**
     * 게시글 삭제
     *
     * @param id
     * @throws Exception
     */
    public void delete(int id) throws Exception {
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            String sql = "DELETE FROM board WHERE board_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
    }

    /**
     * 게시글 내용 업데이트
     *
     * @param id
     * @param password
     * @param writer
     * @param title
     * @param content
     * @throws Exception
     */
    public void update(Board board) throws Exception {
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            if (validatePassword(board.getBoardId(), board.getPassword())) {
                LocalDateTime currentTime = LocalDateTime.now();
                String sql = "UPDATE board SET writer = ?, title = ?, content = ?, modified_at = ? " + "WHERE board_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, board.getWriter());
                pstmt.setString(2, board.getTitle());
                pstmt.setString(3, board.getContent());
                pstmt.setTimestamp(4, Timestamp.valueOf(currentTime));
                pstmt.setInt(5, board.getBoardId());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }

    }

    /**
     * 패스워드 확인
     *
     * @param boardId
     * @param enteredPassword
     * @return
     * @throws Exception
     */
    public boolean validatePassword(int boardId, String enteredPassword) throws Exception {
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT password FROM board WHERE board_id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, boardId);
            pstmt.setString(2, enteredPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return true;
            } else {
//                throw new ValidatorException("비밀번호가 틀립니다.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
        return false;
    }

    /**
     * 입력된 검색조건으로 SQL 생성
     *
     * @param sqlBuilder
     * @param params
     * @param startDate
     * @param endDate
     * @param category
     * @param search
     */
    private void applySearchConditions(StringBuilder sqlBuilder, List<Object> params,
                                       LocalDate startDate, LocalDate endDate, String category, String search) {

        //특정 카테고리 선택 시
        if (!StringUtils.isNullOrEmpty(category)  && !category.equalsIgnoreCase("all")) {
            sqlBuilder.append(" AND category_id = ?");
            params.add(category);
        }
        //특정 검색어 입력 시
        if (!StringUtils.isNullOrEmpty(search)) {
            sqlBuilder.append(" AND (title LIKE ? OR content LIKE ? OR writer LIKE ?)");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }
        //등록일 입력 시
        if (startDate != null && endDate != null) {
            sqlBuilder.append(" AND created_at BETWEEN ? AND ?");
            params.add(Timestamp.valueOf(startDate.atStartOfDay()));
            params.add(Timestamp.valueOf(endDate.atStartOfDay().plusDays(1)));
        }
    }

    /**
     * 검색 조건을 만족하는 게시글 검색
     *
     * @param startDate
     * @param endDate
     * @param category
     * @param search
     * @param currentPage
     * @param pageSize
     * @return
     * @throws Exception
     */
    public List<Board> searchBoards(LocalDate startDate, LocalDate endDate, String category,
                                    String search, int currentPage, int pageSize) throws Exception {
        List<Board> boards = new ArrayList<>();
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM board WHERE 1=1");
            List<Object> params = new ArrayList<>();

            applySearchConditions(sqlBuilder, params, startDate, endDate, category, search);

            int offset = (currentPage - 1) * pageSize;
            sqlBuilder.append(" ORDER BY created_at DESC LIMIT ?, ?");
            params.add(offset);
            params.add(pageSize);

            String sql = sqlBuilder.toString();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Board board = new Board();
                board.setCategoryId(rs.getInt("category_id"));
                board.setBoardId(rs.getInt("board_id"));
                board.setWriter(rs.getString("writer"));
                board.setPassword(rs.getString("password"));
                board.setTitle(rs.getString("title"));
                board.setContent(rs.getString("content"));
                board.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                board.setModifiedAt(rs.getTimestamp("modified_at").toLocalDateTime());
                board.setVisitCount(rs.getInt("visit_count"));

                boards.add(board);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
        return boards;
    }

    /**
     * 검색 조건을 만족하는 게시글 개수
     *
     * @param startDate
     * @param endDate
     * @param category
     * @param search
     * @return
     * @throws Exception
     */
    public int countBoards(LocalDate startDate, LocalDate endDate,
                           String category, String search) throws Exception {
        int totalCount = 0;
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            StringBuilder sqlBuilder = new StringBuilder("SELECT COUNT(*) FROM board WHERE 1=1");
            List<Object> params = new ArrayList<>();

            applySearchConditions(sqlBuilder, params, startDate, endDate, category, search);

            String sql = sqlBuilder.toString();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalCount = rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
        return totalCount;

    }

    /**
     * 게시글 ID로 내용 가져오기
     *
     * @param id
     * @return
     * @throws Exception
     */

    public Board getBoardById(int id) throws Exception {
        Board board = null;
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            String sql = "SELECT * FROM board WHERE board_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                board = new Board();
                board.setBoardId(rs.getInt("board_id"));
                board.setCategoryId(rs.getInt("category_id"));
                board.setWriter(rs.getString("writer"));
                board.setPassword(rs.getString("password"));
                board.setTitle(rs.getString("title"));
                board.setContent(rs.getString("content"));
                board.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                board.setModifiedAt(rs.getTimestamp("modified_at").toLocalDateTime());
                board.setVisitCount(rs.getInt("visit_count"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
        return board;
    }

    /**
     * 게시글 조회수 증가
     *
     * @param id
     * @param visitCount
     * @throws Exception
     */
    public void updateVisitCount(int id, int visitCount) throws Exception {
        Connection conn = null;
        try {
            conn = DBUtils.getConnection();
            String sql = "UPDATE board SET visit_count = ? WHERE board_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, visitCount + 1);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeConnection(conn);
        }
    }
}
