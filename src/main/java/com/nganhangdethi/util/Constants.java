package com.nganhangdethi.util;

import java.awt.Font; // Cần thiết nếu bạn muốn định nghĩa hằng số Font ở đây

public final class Constants {

    // Private constructor để ngăn việc tạo instance của lớp tiện ích này
    private Constants() {
        throw new IllegalStateException("Utility class: Cannot be instantiated");
    }

    // --- Thông tin ứng dụng ---
    public static final String APP_NAME = "Ngân Hàng Đề Thi Tiếng Nhật";
    public static final String APP_VERSION = "1.0.0";

    // --- Thuộc tính câu hỏi & Đề Thi ---
    public static final String ALL_FILTER_OPTION = "Tất cả"; // Dùng cho các ComboBox lọc
    public static final String[] QUESTION_LEVELS = {ALL_FILTER_OPTION, "N1", "N2", "N3", "N4", "N5"};
    public static final String[] QUESTION_TYPES = {ALL_FILTER_OPTION, "Kanji", "Goi", "Bunpou", "Dokkai", "Choukai"};
    public static final String[] CORRECT_ANSWERS = {"A", "B", "C", "D"};

    // --- Loại file ---
    public static final String[] AUDIO_EXTENSIONS = {"mp3", "wav", "m4a", "ogg"};
    public static final String AUDIO_FILE_DESCRIPTION = "Tệp Âm thanh (*.mp3, *.wav, *.m4a, *.ogg)";
    public static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp"};
    public static final String IMAGE_FILE_DESCRIPTION = "Tệp Hình ảnh (*.jpg, *.jpeg, *.png, *.gif, *.bmp)";
    public static final String PDF_EXTENSION = "pdf";
    public static final String PDF_FILE_DESCRIPTION = "Tệp PDF (*.pdf)";
    public static final String DOCX_EXTENSION = "docx";
    public static final String DOCX_FILE_DESCRIPTION = "Tệp Word (*.docx)";


    // --- Kích thước UI ---
    public static final int DEFAULT_DIALOG_WIDTH = 700;
    public static final int DEFAULT_DIALOG_HEIGHT = 750;
    public static final int QUESTION_DIALOG_WIDTH = 800;
    public static final int QUESTION_DIALOG_HEIGHT = 780;
    public static final int EXAM_DIALOG_WIDTH = 950;
    public static final int EXAM_DIALOG_HEIGHT = 700;
    public static final int VIEW_QUESTION_DIALOG_WIDTH = 750;
    public static final int VIEW_QUESTION_DIALOG_HEIGHT = 700;
    public static final int VIEW_EXAM_DIALOG_WIDTH = 850;
    public static final int VIEW_EXAM_DIALOG_HEIGHT = 700;
    public static final int SETTINGS_DIALOG_WIDTH = 500;
    public static final int SETTINGS_DIALOG_HEIGHT = 200;


    // --- Thông báo mặc định ---
    public static final String MSG_SAVE_SUCCESS = "Lưu thành công!";
    public static final String MSG_SAVE_FAIL = "Lưu thất bại. Vui lòng thử lại.";
    public static final String MSG_UPDATE_SUCCESS = "Cập nhật thành công!";
    public static final String MSG_UPDATE_FAIL = "Cập nhật thất bại. Vui lòng thử lại.";
    public static final String MSG_DELETE_SUCCESS = "Xóa thành công!";
    public static final String MSG_DELETE_FAIL = "Xóa thất bại. Vui lòng thử lại.";
    public static final String MSG_CONFIRM_TITLE = "Xác nhận"; // Tiêu đề chung cho dialog xác nhận
    public static final String MSG_CONFIRM_DELETE_TITLE = "Xác nhận xóa";
    public static final String MSG_CONFIRM_DELETE_QUESTION = "Bạn có chắc chắn muốn xóa câu hỏi này?";
    public static final String MSG_CONFIRM_DELETE_EXAM = "Bạn có chắc chắn muốn xóa đề thi này?\nTất cả liên kết câu hỏi trong đề cũng sẽ bị xóa.";
    public static final String MSG_NO_ITEM_SELECTED = "Vui lòng chọn một mục để thực hiện thao tác.";
    public static final String MSG_NO_ITEM_SELECTED_FOR_EDIT = "Vui lòng chọn một mục để sửa.";
    public static final String MSG_NO_ITEM_SELECTED_FOR_DELETE = "Vui lòng chọn một mục để xóa.";
    public static final String MSG_NO_ITEM_SELECTED_FOR_VIEW = "Vui lòng chọn một mục để xem chi tiết.";
    public static final String MSG_NO_ITEM_SELECTED_FOR_EXPORT = "Vui lòng chọn một đề thi để xuất file.";
    public static final String MSG_REQUIRED_FIELD_MISSING = "Vui lòng điền đầy đủ các trường có dấu (*).";
    public static final String MSG_INVALID_INPUT = "Dữ liệu nhập không hợp lệ.";
    public static final String MSG_API_KEY_MISSING = "Chưa cấu hình API Key cho OpenAI. Vui lòng vào Cài đặt.";
    public static final String MSG_API_REQUEST_ERROR = "Có lỗi xảy ra khi gửi yêu cầu đến AI.";
    public static final String MSG_EXPORT_SUCCESS = "Xuất file thành công!";
    public static final String MSG_EXPORT_FAIL = "Xuất file thất bại. Vui lòng kiểm tra lại.";
    public static final String MSG_ERROR_LOADING_DATA = "Lỗi khi tải dữ liệu từ cơ sở dữ liệu.";
    public static final String MSG_UNEXPECTED_ERROR = "Đã có lỗi không mong muốn xảy ra. Vui lòng thử lại hoặc liên hệ hỗ trợ.";
    public static final String MSG_CONFIRM_SAVE_EMPTY_EXAM = "Đề thi chưa có câu hỏi nào. Bạn có muốn tiếp tục lưu không?";

    // --- Font ---
    public static final String DEFAULT_FONT_NAME_IN_FILE = "NotoSansJP-Regular"; // Tên file font (không có phần mở rộng)
    public static final String FONT_RESOURCE_PATH = "/fonts/NotoSansJP-Regular.ttf"; // Đường dẫn trong resources

    // --- Tên cột trong JTable ---
    public static final String COL_ID = "ID";
    public static final String COL_QUESTION_SUMMARY = "Nội dung (tóm tắt)";
    public static final String COL_LEVEL = "Cấp độ";
    public static final String COL_TYPE = "Loại";
    public static final String COL_CORRECT_ANSWER = "Đáp án";
    public static final String COL_TAGS = "Tags";
    public static final String COL_AUDIO = "Audio";
    public static final String COL_IMAGE = "Image";
    public static final String COL_EXAM_NAME = "Tên Đề Thi";
    public static final String COL_EXAM_LEVEL_TARGET = "Cấp độ Mục tiêu";
    public static final String COL_EXAM_DESCRIPTION_SUMMARY = "Mô tả (tóm tắt)";
    public static final String COL_EXAM_NUM_QUESTIONS = "Số câu";
    public static final String COL_CREATED_AT = "Ngày tạo";
    public static final String COL_ORDER_IN_EXAM = "STT"; // Cho bảng câu hỏi trong đề thi


    // --- Action Commands (cho xử lý sự kiện) ---
    // (Đã khá đầy đủ từ phiên bản trước của bạn, giữ nguyên nếu thấy phù hợp)
    public static final String AC_ADD = "ADD_ACTION";
    public static final String AC_EDIT = "EDIT_ACTION";
    public static final String AC_DELETE = "DELETE_ACTION";
    public static final String AC_VIEW_DETAIL = "VIEW_DETAIL_ACTION";
    public static final String AC_REFRESH = "REFRESH_ACTION";
    public static final String AC_SEARCH = "SEARCH_ACTION";
    public static final String AC_FILTER_CHANGED = "FILTER_CHANGED_ACTION"; // Khi giá trị filter thay đổi
    public static final String AC_SAVE = "SAVE_ACTION";
    public static final String AC_CANCEL = "CANCEL_ACTION";
    public static final String AC_BROWSE_AUDIO = "BROWSE_AUDIO_ACTION";
    public static final String AC_PLAY_AUDIO = "PLAY_AUDIO_ACTION";
    public static final String AC_STOP_AUDIO = "STOP_AUDIO_ACTION";
    public static final String AC_CLEAR_AUDIO = "CLEAR_AUDIO_ACTION";
    public static final String AC_BROWSE_IMAGE = "BROWSE_IMAGE_ACTION";
    public static final String AC_CLEAR_IMAGE = "CLEAR_IMAGE_ACTION";
    public static final String AC_AI_SUGGEST = "AI_SUGGEST_ACTION";
    public static final String AC_EXPORT_PDF = "EXPORT_PDF_ACTION";
    public static final String AC_EXPORT_DOCX = "EXPORT_DOCX_ACTION";
    public static final String AC_EXPORT_ANSWERS_PDF = "EXPORT_ANSWERS_PDF_ACTION";
    public static final String AC_EXPORT_ANSWERS_DOCX = "EXPORT_ANSWERS_DOCX_ACTION";
    public static final String AC_ADD_QUESTION_TO_EXAM = "ADD_Q_TO_EXAM";
    public static final String AC_REMOVE_QUESTION_FROM_EXAM = "REMOVE_Q_FROM_EXAM";
    public static final String AC_MOVE_QUESTION_UP = "MOVE_Q_UP";
    public static final String AC_MOVE_QUESTION_DOWN = "MOVE_Q_DOWN";


    // --- ICON PATHS (đường dẫn tương đối từ thư mục resources) ---
    // Đảm bảo các file này tồn tại trong src/main/resources/icons/
    public static final String ICON_PATH_APP_ICON = "/icons/app_icon.png"; // Icon chính của ứng dụng (nếu có)
    public static final String ICON_PATH_SAVE = "/icons/save.png";
    public static final String ICON_PATH_ADD = "/icons/add.png";
    public static final String ICON_PATH_EDIT = "/icons/edit.png";
    public static final String ICON_PATH_DELETE = "/icons/delete.png";
    public static final String ICON_PATH_CANCEL = "/icons/cancel.png";
    public static final String ICON_PATH_SEARCH = "/icons/search.png";
    public static final String ICON_PATH_REFRESH = "/icons/refresh.png";
    public static final String ICON_PATH_VIEW = "/icons/view_detail.png";
    public static final String ICON_PATH_SETTINGS = "/icons/settings.png";
    public static final String ICON_PATH_EXIT = "/icons/exit.png";
    public static final String ICON_PATH_ABOUT = "/icons/about.png";
    public static final String ICON_PATH_FILTER = "/icons/filter.png";
    public static final String ICON_PATH_QUESTION_TAB = "/icons/question_tab.png"; // Đổi tên cho rõ ràng
    public static final String ICON_PATH_EXAM_TAB = "/icons/exam_tab.png";         // Đổi tên cho rõ ràng
    public static final String ICON_PATH_PLAY_AUDIO = "/icons/play_audio.png";
    public static final String ICON_PATH_STOP_AUDIO = "/icons/stop_audio.png";
    public static final String ICON_PATH_BROWSE_FILE = "/icons/browse_file.png";
    public static final String ICON_PATH_CLEAR_FILE = "/icons/clear_file.png"; // Icon cho nút xóa file đã chọn
    public static final String ICON_PATH_EXPORT_PDF = "/icons/export_pdf.png";
    public static final String ICON_PATH_EXPORT_WORD = "/icons/export_word.png";
    public static final String ICON_PATH_AI_SUGGEST = "/icons/ai_suggestion.png";
    public static final String ICON_PATH_UPLOAD = "/icons/upload.png";
    public static final String ICON_PATH_MOVE_UP = "/icons/move_up.png";
    public static final String ICON_PATH_MOVE_DOWN = "/icons/move_down.png";
    public static final String ICON_PATH_ADD_TO_LIST = "/icons/add_to_list.png"; // Ví dụ cho nút >>
    public static final String ICON_PATH_REMOVE_FROM_LIST = "/icons/remove_from_list.png"; // Ví dụ cho nút <<
    public static final String ICON_PATH_SHUFFLE = "/icons/mix.png";
    public static final String ICON_PATH_BATCH_ADD_IMAGE = "/icons/batch_add_image.png"; // ICON MỚI
    


    // --- Keys cho Properties/Configuration ---
    // (Đã khá đầy đủ từ phiên bản trước của bạn, giữ nguyên)
    public static final String PROP_DB_URL_BASE = "db.url.base";
    public static final String PROP_DB_NAME = "db.name";
    public static final String PROP_DB_USER = "db.user";
    public static final String PROP_DB_PASSWORD = "db.password";
    public static final String PROP_DB_PARAMS = "db.params";
    public static final String PROP_AI_OPENAI_APIKEY = "ai.openai.apikey";
    public static final String PROP_APP_BASE_DIR = "app.base.dir";
    public static final String PROP_APP_AUDIO_DIR = "app.audio.dir";
    public static final String PROP_APP_IMAGE_DIR = "app.image.dir";
    public static final String[] QUESTION_LEVELS_NO_ALL = {"N1", "N2", "N3", "N4", "N5"};
    public static final String[] QUESTION_TYPES_NO_ALL = {"Kanji", "Goi", "Bunpou", "Dokkai-Passage", "Dokkai-SubQuestion", "Choukai-Group", "Choukai-SubQuestion", "ImageBased"};
    public static final String DEFAULT_QUESTION_LEVEL = "N5";
    public static final String DEFAULT_QUESTION_TYPE = "Kanji";
 // Thêm vào file com/nganhangdethi/util/Constants.java

 // ... các hằng số khác ...
 public static final String TITLE_CREATE_MULTIPLE_QUESTIONS = "Tạo Nhiều Câu Hỏi từ File Ảnh";
 public static final String LABEL_CHOOSE_IMAGE_FILE = "Chọn File Ảnh...";
 public static final String LABEL_AI_INSTRUCTIONS_MULTI = "Hướng dẫn thêm cho AI (tùy chọn):";
 public static final String LABEL_NUM_QUESTIONS_TO_EXTRACT = "Số câu hỏi dự kiến trích xuất:";
 public static final String BUTTON_EXTRACT_WITH_AI = "Trích xuất bằng AI";
 public static final String BUTTON_ADD_SELECTED_TO_BANK = "Thêm mục đã chọn vào Ngân hàng";
 public static final String MSG_NO_FILE_SELECTED_FOR_AI = "Vui lòng chọn một file ảnh để AI xử lý.";
 public static final String MSG_AI_EXTRACTION_SUCCESS = "AI đã trích xuất %d câu hỏi. Vui lòng xem lại.";
 public static final String MSG_AI_EXTRACTION_FAIL = "AI trích xuất thất bại hoặc không tìm thấy câu hỏi.";
 public static final String MSG_NO_QUESTIONS_SELECTED_FOR_BANK = "Vui lòng chọn ít nhất một câu hỏi để thêm vào ngân hàng.";
 public static final String MSG_CONFIRM_CANCEL_MULTI_ADD = "Bạn có chắc muốn hủy? Các câu hỏi AI tạo ra chưa lưu sẽ bị mất.";

 public static final String COL_SELECT = "Chọn"; // Cho JTable trong CreateMultipleQuestionsDialog
 public static final String COL_EDIT_ACTION = "Sửa";
 public static final String COL_DELETE_ACTION = "Xóa";
 public static final String MSG_CANCEL_ACTION = "Hủy";
 // ...
    public static final String AI_JSON_FORMAT_FULL_QUESTION = "\nTrả lời theo định dạng JSON sau:\n" +
                             "{\n" +
                             "  \"question_text\": \"(Nội dung câu hỏi ở đây)\",\n" +
                             "  \"option_a\": \"(Lựa chọn A)\",\n" +
                             "  \"option_b\": \"(Lựa chọn B)\",\n" +
                             "  \"option_c\": \"(Lựa chọn C)\",\n" +
                             "  \"option_d\": \"(Lựa chọn D)\",\n" +
                             "  \"correct_answer\": \"(Chỉ một chữ cái A, B, C, hoặc D)\",\n" +
                             "  \"explanation\": \"(Giải thích ngắn gọn cho đáp án đúng)\"\n" +
                             "}";
    public static final String AI_JSON_FORMAT_DISTRACTORS = "\nTrả lời theo định dạng JSON với 3 lựa chọn sai:\n" +
                             "{\n" +
                             "  \"distractor_1\": \"(Lựa chọn sai 1)\",\n" +
                             "  \"distractor_2\": \"(Lựa chọn sai 2)\",\n" +
                             "  \"distractor_3\": \"(Lựa chọn sai 3)\"\n" +
                             "}";

    public static final String AI_PROMPT_EXTRACT_MULTIPLE_QUESTIONS_FROM_IMAGE = "\n" +
            "Bạn là một chuyên gia về đề thi JLPT tiếng Nhật. Phân tích kỹ hình ảnh được cung cấp, đây là một trang đề thi tiếng Nhật.\n" +
            "Nhiệm vụ của bạn là xác định và trích xuất {NUM_QUESTIONS} câu hỏi trắc nghiệm riêng biệt từ hình ảnh.\n" +
            "Đối với MỖI câu hỏi bạn trích xuất được, hãy cung cấp đầy đủ các thông tin sau:\n" +
            "1. `question_text`: Toàn bộ nội dung câu hỏi, bao gồm cả các ví dụ, đoạn văn hoặc ngữ cảnh liên quan (nếu có).\n" +
            "2. `option_a`: Nội dung của lựa chọn A.\n" +
            "3. `option_b`: Nội dung của lựa chọn B.\n" +
            "4. `option_c`: Nội dung của lựa chọn C (nếu có, nếu không thì để trống).\n" +
            "5. `option_d`: Nội dung của lựa chọn D (nếu có, nếu không thì để trống).\n" +
            "6. `correct_answer`: Chỉ một chữ cái IN HOA (A, B, C, hoặc D) của đáp án đúng.\n" +
            "7. `explanation`: Giải thích ngắn gọn, rõ ràng cho đáp án đúng (nếu có thể suy luận hoặc trích xuất).\n" +
            "8. `level`: Ước tính và chỉ định cấp độ JLPT (chỉ một trong các giá trị: N1, N2, N3, N4, N5) cho câu hỏi này.\n" +
            "9. `type`: Xác định và chỉ định loại câu hỏi (chỉ một trong các giá trị sau, chọn loại phù hợp nhất: " +
            String.join(", ", QUESTION_TYPES_NO_ALL) +
            ").\n" +
            "\n" +
            "YÊU CẦU QUAN TRỌNG VỀ ĐỊNH DẠNG PHẢN HỒI:\n" +
            "Hãy trả lời bằng một MẢNG JSON (JSON array). Mỗi phần tử của mảng phải là một ĐỐI TƯỢNG JSON (JSON object) đại diện cho MỘT câu hỏi đã trích xuất.\n" +
            "Tuyệt đối KHÔNG bao gồm bất kỳ văn bản nào khác ngoài mảng JSON này trong phản hồi của bạn (không giải thích, không mở đầu, không kết luận).\n" +
            "Ví dụ về cấu trúc một đối tượng câu hỏi trong mảng:\n" +
            "{\n" +
            "  \"question_text\": \"「山川」の読み方はどれですか。\",\n" +
            "  \"option_a\": \"やまかわ\",\n" +
            "  \"option_b\": \"やまがわ\",\n" +
            "  \"option_c\": \"さんせん\",\n" +
            "  \"option_d\": \"さんかわ\",\n" +
            "  \"correct_answer\": \"A\",\n" +
            "  \"explanation\": \"「山川」は通常「やまかわ」と読みますが、文脈によっては「やまがわ」も可能です。Tuy nhiên, trong các lựa chọn thường「やまかわ」 là đáp án chính xác cho trình độ cơ bản.\",\n" +
            "  \"level\": \"N4\",\n" +
            "  \"type\": \"Kanji\"\n" +
            "}\n" +
            "Nếu bạn tìm thấy ít hơn {NUM_QUESTIONS} câu hỏi, hãy trích xuất tất cả những câu bạn tìm được. " +
            "Nếu bạn tìm thấy nhiều hơn, hãy ưu tiên {NUM_QUESTIONS} câu hỏi rõ ràng nhất, theo thứ tự từ trên xuống dưới, từ trái qua phải trong ảnh.\n" +
            "Đảm bảo toàn bộ phản hồi của bạn là một mảng JSON hợp lệ, bắt đầu bằng `[` và kết thúc bằng `]`.\n" +
            "Yêu cầu thêm từ người dùng: {USER_INSTRUCTIONS}";

}