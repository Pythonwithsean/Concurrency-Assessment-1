import java.util.ArrayList;

public class TestBinarySearch {

    public Integer getFileIndex(ArrayList<String> files, String fileName) {
        int l = 0;
        int r = files.size() - 1;
        files.sort(String::compareTo);
        while (l <= r) {
            int mid = (r + l) / 2;
            int cmp = files.get(mid).compareTo(fileName);
            if (cmp == 0) {
                return mid;
            } else if (cmp < 0) {
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        TestBinarySearch ts = new TestBinarySearch();
        ArrayList<String> names = new ArrayList<>();
        names.add("Sean.txt");
        names.add("Daniel.txt");
        names.add("James.txt");
        names.add("Sabella.txt");
        names.add("Nadia.txt");
        names.add("sabella.txt");
        System.out.println(names);
        System.out.println(ts.getFileIndex(names, "Hello.txt")); // Expected -1
        System.out.println(ts.getFileIndex(names, "Sean.txt")); // Expected 4
        System.out.println(ts.getFileIndex(names, "James.txt")); // Expected 1
        System.out.println(ts.getFileIndex(names, "sabella.txt")); // Expected 5
    }

}
