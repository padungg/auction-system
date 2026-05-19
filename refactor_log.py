import sys, re

def process(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Add imports
    if 'org.slf4j.Logger' not in content:
        content = re.sub(r'(package [a-z0-9.]+;)', r'\1\n\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;', content)
        
    class_name_match = re.search(r'public class (\w+)', content)
    if class_name_match:
        class_name = class_name_match.group(1)
        if 'Logger LOGGER' not in content:
            content = re.sub(r'(public class ' + class_name + r'[^\{]*\{)', r'\1\n    private static final Logger LOGGER = LoggerFactory.getLogger(' + class_name + r'.class);', content)

    # 1. Replace System.err.println("... " + e.getMessage());
    content = re.sub(r'System\.err\.println\(\s*"([^"]+)"\s*\+\s*([a-zA-Z0-9_.]+)\.getMessage\(\)\s*\);', r'LOGGER.error("\1{}", \2.getMessage(), \2);', content)
    
    # 2. Specific case in AuctionDAOImpl
    content = content.replace(
        'System.err.println(">>> [AuctionDAO] Cảnh báo: Trạng thái không hợp lệ trong DB: " + statusStr + ". Đã đổi thành CANCELED.");',
        'LOGGER.warn(">>> [AuctionDAO] Cảnh báo: Trạng thái không hợp lệ trong DB: {}. Đã đổi thành CANCELED.", statusStr);'
    )
    
    # 3. DatabaseConnection driver error
    content = content.replace(
        'System.err.println(">>> [DB] LỖI: Không tìm thấy MySQL Driver!");',
        'LOGGER.error(">>> [DB] LỖI: Không tìm thấy MySQL Driver!");'
    )
    
    # 4. DatabaseInitializer error
    content = content.replace(
        'System.err.println(">>> [DB] LỖI khởi tạo bảng: " + e.getMessage());',
        'LOGGER.error(">>> [DB] LỖI khởi tạo bảng: {}", e.getMessage(), e);'
    )

    # 5. DatabaseInitializer success message which might be multiline
    content = content.replace(
        'System.out.println(\n                                ">>> [DB] Đã khởi tạo cấu trúc CSDL thành công!");',
        'LOGGER.info(">>> [DB] Đã khởi tạo cấu trúc CSDL thành công!");'
    )
    
    # 6. Any other System.out.println("...")
    content = re.sub(r'System\.out\.println\(\s*"([^"]+)"\s*\);', r'LOGGER.info("\1");', content)

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

files = [
    "src/main/java/com/auction/server/dao/UserDAOImpl.java",
    "src/main/java/com/auction/server/dao/ItemDAOImpl.java",
    "src/main/java/com/auction/server/dao/BidTransactionDAOImpl.java",
    "src/main/java/com/auction/server/dao/AutoBidDAOImpl.java",
    "src/main/java/com/auction/server/dao/AuctionDAOImpl.java",
    "src/main/java/com/auction/server/database/DatabaseInitializer.java",
    "src/main/java/com/auction/server/database/DatabaseConnection.java"
]

for f in files:
    try:
        process(f)
        print(f"Processed {f}")
    except Exception as e:
        print(f"Failed {f}: {e}")
