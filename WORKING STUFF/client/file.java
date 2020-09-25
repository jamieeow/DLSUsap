public class file {

        private byte[] data;
        private String name;

        public byte[] getData() {
            return data;
        }

        public String getName() {
            return name;
        }

        public file(int length, String fileName) {
            data = new byte[length];
            name = fileName;
        }
}