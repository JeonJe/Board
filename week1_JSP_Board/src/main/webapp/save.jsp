<%--
  Created by IntelliJ IDEA.
  User: premise
  Date: 2023/05/16
  Time: 11:37 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page import="board.Board" %>
<%@ page import="board.BoardDAO" %>

<%@page import="com.oreilly.servlet.multipart.DefaultFileRenamePolicy" %>
<%@page import="com.oreilly.servlet.MultipartRequest" %>
<%@ page import="java.util.Enumeration" %>

<%@ page import="attachment.Attachment" %>
<%@ page import="attachment.AttachmentDAO" %>
<%@ page import="java.io.File" %>
<%@ page import="utils.BoardUtils" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // 업로드할 서버 디렉토리 위치
    String uploadPath = "/Users/premise/Desktop/github/Java/ebrain/upload";

    File dir = new File(uploadPath);
    if (!dir.exists()) {
        dir.mkdir();
    }

    // 첨부파일의 크기(단위:Byte) : 10MB
    int size = 10 * 1024 * 1024;

    // 첨부파일은 MultipartRequest 객체를 생성하면서 업로드 된다.
    try {
        MultipartRequest multi = new MultipartRequest(request,
                uploadPath,
                size,
                "utf-8",
                new DefaultFileRenamePolicy());

        String category = multi.getParameter("category_id");
        String writer = multi.getParameter("writer");
        String password = multi.getParameter("password");
        String passwordConfirm = multi.getParameter("password-confirm");
        String title = multi.getParameter("title");
        String content = multi.getParameter("content");
        String hashedPassword = BoardUtils.hashPassword(password);

        boolean isValid = BoardUtils.checkFormValidation(writer, password, passwordConfirm, title, content);
        if (!isValid) {
            return;
        }

        //게시글 저장
        Board board = new Board();
        board.setCategoryId(Integer.parseInt(category));
        board.setWriter(writer);
        board.setPassword(hashedPassword);
        board.setTitle(title);
        board.setContent(content);
        board.setVisitCount(0);

        BoardDAO boardDAO = new BoardDAO();
        int boardId = boardDAO.save(board);
        //첨부 파일 저장
        Enumeration files = multi.getFileNames();

        while (files.hasMoreElements()) {
            String file = (String) files.nextElement();
            String fileName = multi.getFilesystemName(file);
            System.out.println(fileName);
            if (fileName != null) {
                Attachment attachment = new Attachment(boardId, fileName, fileName);
                AttachmentDAO attachmentDAO = new AttachmentDAO();
                attachmentDAO.save(attachment);
            }
        }

        response.sendRedirect("list.jsp");

    } catch (Exception e) {
        e.printStackTrace();
    }

%>