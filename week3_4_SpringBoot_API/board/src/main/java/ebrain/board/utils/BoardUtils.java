package ebrain.board.utils;

import ebrain.board.response.APIResponse;
import ebrain.board.vo.AttachmentVO;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
/**
 * 첨부 파일을 다운로드하거나 API 응답을 생성하는 게시판 관련 유틸리티 클래스입니다.
 */
public class BoardUtils {


    /**
     * 폼 유효성을 검사하는 메서드
     *
     * @param writer         작성자
     * @param password       비밀번호
     * @param passwordConfirm 비밀번호 확인
     * @param title          제목
     * @param content        내용
     * @return 폼 유효성 검사 결과
     */
    public static boolean checkFormValidation(String writer, String password, String passwordConfirm, String title, String content) {
        try {

            if (writer.length() < 3 || writer.length() >= 5) {
                return false;
            }
            if (password.length() < 4 || password.length() >= 16 || !password.equals(passwordConfirm)) {
                return false;
            }
            String passwordRegex = "(?i)^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$";
            if (!password.matches(passwordRegex)) {
                return false;
            }
            if (title.length() < 4 || title.length() >= 100) {
                return false;
            }
            if (content.length() < 4 || content.length() >= 2000) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }



    /**
     * 비밀번호를 해싱하는 메서드
     *
     * @param password 비밀번호
     * @return 해시화된 비밀번호
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashedPassword = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hashedPassword.append(String.format("%02x", hashByte));
            }
            return hashedPassword.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 파일을 업로드하는 메서드
     *
     * @param file        업로드할 파일
     * @param uploadPath  파일 업로드 경로
     * @return 업로드된 파일의 이름
     * @throws Exception 파일 업로드 중 발생한 예외
     */
    //TODO : 이름만 넘겨주기보다 파일로 넘겨서 나중에 사용하기 용이하게 처리
    public static String uploadFile(MultipartFile file, String uploadPath) throws Exception {
        String fileName = file.getOriginalFilename();
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);

        // 중복 파일명 처리합니다.
        File uploadedFile = new File(uploadPath + File.separator + fileName);

        int count = 1;
        while (uploadedFile.exists()) {
            // 중복 파일명에 번호 추가합니다.
            String numberedFileName = baseName + "_" + count + "." + extension;
            uploadedFile = new File(uploadPath + File.separator + numberedFileName);
            count++;
        }

        //파일을 업로드 폴더로 업로드합니다.
        file.transferTo(uploadedFile);
        //파일 고유 식별번호를 반환합니다.
        return uploadedFile.getName();
//        return uploadedFile;
    }
    /**
     * 첨부 파일을 다운로드하기 위한 ResponseEntity를 생성합니다.
     *
     * @param attachment   다운로드할 첨부 파일 정보
     * @param UPLOAD_PATH  파일 업로드 경로
     * @return 파일 다운로드를 위한 ResponseEntity
     * @throws IOException IO 예외 발생 시
     */
    public static ResponseEntity<Resource> fileDownload(AttachmentVO attachment, String UPLOAD_PATH) throws IOException {
        // 파일을 읽어올 InputStream을 생성합니다.
        String filePath = UPLOAD_PATH + File.separator + attachment.getFileName();
        File file = new File(filePath);

        // 파일이 존재하는지 확인합니다.
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        // 파일 다운로드를 위한 Response Header를 설정합니다.
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + attachment.getFileName());

        // 파일의 MIME 타입을 설정합니다.
        String contentType = Files.probeContentType(file.toPath());
        headers.setContentType(MediaType.parseMediaType(contentType));

        // 파일의 크기를 설정합니다.
        long contentLength = file.length();
        headers.setContentLength(contentLength);
        // 파일 다운로드 응답을 생성하여 반환합니다.
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    /**
     * 잘못된 요청에 대한 API 응답을 생성합니다.
     *
     * @param message 응답 메시지
     * @return 잘못된 요청에 대한 ResponseEntity
     */
    public static ResponseEntity<APIResponse> createBadRequestResponse(String message) {
        APIResponse apiResponse = new APIResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(message);
        return ResponseEntity.badRequest().body(apiResponse);
    }

    /**
     * 성공적인 API 응답을 생성합니다.
     *
     * @param data 응답 데이터
     * @return 성공적인 API 응답에 대한 ResponseEntity
     */
    public static ResponseEntity<APIResponse> createOkResponse(Object data) {
        APIResponse apiResponse = new APIResponse();
        apiResponse.setSuccess(true);
        apiResponse.setMessage("Success");
        apiResponse.setData(data);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 내부 서버 오류에 대한 API 응답을 생성합니다.
     *
     * @param errorMessage 오류 메시지
     * @return 내부 서버 오류에 대한 ResponseEntity
     */
    public static ResponseEntity<APIResponse> createInternalServerErrorResponse(String errorMessage) {
        APIResponse apiResponse = new APIResponse();
        apiResponse.setSuccess(false);
        apiResponse.setMessage(errorMessage);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
