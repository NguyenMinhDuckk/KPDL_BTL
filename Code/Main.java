import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        ClearScreen();
        
        List<Set<String>> data = loadDatasetFromCSV("./Data/sample.csv");
        // List<Set<String>> data = loadDatasetFromCSV("./Data/supermarket.csv");
        
        if (data == null) System.exit(0);  // nếu không đọc được file thì dừng lại và in ra lỗi

        double minSupport = 0.3;
        double minConfidence = 1;

        Map<Set<String>, Double> frequentItemsets = apriori(data, minSupport);
        List<AssociationRule> associationRules = generateAssociationRules(frequentItemsets, minConfidence);

        printFrequentItemsets(frequentItemsets);
        printAssociationRules(associationRules);
    }

    public static void printFrequentItemsets(Map<Set<String>, Double> frequentItemsets) {
        // Sorting frequentItemsets based on size and lexicographic order
        List<Map.Entry<Set<String>, Double>> sortedFrequentItemsets = new ArrayList<>(frequentItemsets.entrySet());
        sortedFrequentItemsets.sort(itemsetSizeComparator);

        int itemsetColumnWidth = 45;
        String separator = "-----------------------------------------------------------------";
        String separatorPoint = "|-----+-----------------------------------------------+---------|";

        int stt = 1;

        System.out.println("Frequent Itemsets:");
        System.out.println(separator);
        System.out.printf("| %-2s | %-" + itemsetColumnWidth + "s | %s | %n", "STT", "Itemset", "Support");
        System.out.println(separatorPoint);

        for (Map.Entry<Set<String>, Double> entry : sortedFrequentItemsets) {
            Set<String> itemset = entry.getKey();
            double support = entry.getValue();

            String itemsetStr = String.join(", ", itemset);
            System.out.printf("| %-2d  | %-" + itemsetColumnWidth + "s | %.2f%%  | %n", stt, "[" + itemsetStr + "]",
                    support * 100);

            stt++;
        }
        System.out.println(separator);
    }

    public static void printAssociationRules(List<AssociationRule> associationRules) {
        associationRules.sort((rule1, rule2) -> {
            int size1 = rule1.getAntecedent().size();
            int size2 = rule2.getAntecedent().size();

            if (size1 != size2) {
                return Integer.compare(size1, size2);
            }

            return rule1.getAntecedent().toString().compareTo(rule2.getAntecedent().toString());
        });
        System.out.println();
        System.out.println();
        String separator = "-------------------------------------------------------------------------------";
        System.out.println("Association Rules:");
        System.out.println(separator);
        for (AssociationRule rule : associationRules) {
            System.out.println(rule);
        }
        System.out.println(separator);
    }
    public static List<Set<String>> loadDatasetFromCSV(String filename) {
        List<Set<String>> data = new ArrayList<>();

        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);
            // Đọc dòng tiêu đề và lấy danh sách các mục
            String header = scanner.nextLine();
            String[] headers = header.split(",");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] row = line.split(",");

                Set<String> transaction = new HashSet<>();
                for (int i = 0; i < row.length; i++) {
                    if (row[i].equals("t")) {
                        transaction.add(headers[i]);
                    }
                }

                data.add(transaction);
            }

            scanner.close();
        } catch (IOException e) {
            System.out.println("Cannot read file. Please check your path!");
            return null;
        }
        return data;
    }

    public static Map<Set<String>, Double> apriori(List<Set<String>> data, double minSupport) {
        Map<Set<String>, Double> frequentItemsets = new HashMap<>();
        int k = 1;
        Set<Set<String>> itemsets = createInitialItemsets(data);

        while (!itemsets.isEmpty()) {
            Map<Set<String>, Double> support = calculateSupport(data, itemsets);
            for (Map.Entry<Set<String>, Double> entry : support.entrySet()) {
                if (entry.getValue() >= minSupport) {
                    frequentItemsets.put(entry.getKey(), entry.getValue());
                }
            }
            itemsets = joinItemsets(itemsets, k);
            k++;
        }

        return frequentItemsets;
    }

    public static Set<Set<String>> createInitialItemsets(List<Set<String>> data) {
        Set<Set<String>> itemsets = new HashSet<>();
        for (Set<String> transaction : data) {
            for (String item : transaction) {
                itemsets.add(new HashSet<>(Collections.singleton(item)));
            }
        }
        return itemsets;
    }

    public static Map<Set<String>, Double> calculateSupport(List<Set<String>> data, Set<Set<String>> itemsets) {
        int numTransactions = data.size();
        Map<Set<String>, Double> support = new HashMap<>();
        for (Set<String> itemset : itemsets) {
            int count = 0;
            for (Set<String> transaction : data) {
                if (transaction.containsAll(itemset)) {
                    count++;
                }
            }
            double supportValue = (double) count / numTransactions;
            support.put(itemset, supportValue);
        }
        return support;
    }

    public static Set<Set<String>> joinItemsets(Set<Set<String>> itemsets, int k) {
        Set<Set<String>> newItems = new HashSet<>();
        List<Set<String>> itemsetList = new ArrayList<>(itemsets);

        for (int i = 0; i < itemsetList.size(); i++) {
            for (int j = i + 1; j < itemsetList.size(); j++) {
                Set<String> item1 = itemsetList.get(i);
                Set<String> item2 = itemsetList.get(j);

                // Candidate Pruning
                Set<String> newItemset = new HashSet<>(item1);
                newItemset.addAll(item2);

                if (newItemset.size() == k + 1) {
                    boolean canJoin = true;
                    List<String> newItemsetList = new ArrayList<>(newItemset);

                    // Check (k-1)-subset containment
                    for (int m = 0; m < k; m++) {
                        Set<String> subset = new HashSet<>(newItemsetList.subList(0, m));
                        subset.addAll(newItemsetList.subList(m + 1, k + 1));

                        if (!itemsets.contains(subset)) {
                            canJoin = false;
                            break;
                        }
                    }

                    if (canJoin) {
                        newItems.add(newItemset);
                    }
                }
            }
        }
        return newItems;
    }

    public static List<AssociationRule> generateAssociationRules(Map<Set<String>, Double> frequentItemsets,
            double minConfidence) {
        List<AssociationRule> associationRules = new ArrayList<>();
        for (Map.Entry<Set<String>, Double> entry : frequentItemsets.entrySet()) {
            Set<String> itemset = entry.getKey();

            if (itemset.size() > 1) {
                List<String> itemsetList = new ArrayList<>(itemset);
                for (int i = 1; i < itemsetList.size(); i++) {
                    List<Set<String>> antecedents = generateCombinations(itemsetList, i);
                    for (Set<String> antecedent : antecedents) {
                        Set<String> consequent = new HashSet<>(itemset);
                        consequent.removeAll(antecedent);
                        double confidence = entry.getValue() / frequentItemsets.get(antecedent);
                        if (confidence >= minConfidence) {
                            associationRules.add(new AssociationRule(antecedent, consequent, confidence));
                        }
                    }
                }
            }
        }
        return associationRules;
    }

    public static List<Set<String>> generateCombinations(List<String> items, int k) {
        List<Set<String>> result = new ArrayList<>();
        List<String> combination = new ArrayList<>();
        generateCombinationsHelper(items, k, 0, result, combination);
        return result;
    }

    public static void generateCombinationsHelper(List<String> items, int k, int start, List<Set<String>> result,
            List<String> combination) {
        if (k == 0) {
            result.add(new HashSet<>(combination));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            combination.add(items.get(i));
            generateCombinationsHelper(items, k - 1, i + 1, result, combination);
            combination.remove(combination.size() - 1);
        }
    }

    public static void ClearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static Comparator<Map.Entry<Set<String>, Double>> itemsetSizeComparator = (entry1, entry2) -> {
        int size1 = entry1.getKey().size();
        int size2 = entry2.getKey().size();

        if (size1 != size2) {
            return Integer.compare(size1, size2);
        }

        return entry1.getKey().toString().compareTo(entry2.getKey().toString());
    };
}


class AssociationRule {
    public static int stt = 0;
    private Set<String> antecedent;
    private Set<String> consequent;
    private double confidence;
    private static boolean printedHeader = false;

    public AssociationRule(Set<String> antecedent, Set<String> consequent, double confidence) {
        this.antecedent = antecedent;
        this.consequent = consequent;
        this.confidence = confidence;
    }

    public Set<String> getAntecedent() {
        return antecedent;
    }

    public Set<String> getConsequent() {
        return consequent;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        String antecedentStr = String.join(", ", antecedent);
        String consequentStr = String.join(", ", consequent);

        int itemsetColumnWidth = 60;
        int confidenceColumnWidth = 10; // Độ rộng của cột Confidence
        stt++;
        String line = String.format("| %2d  | %-" + (itemsetColumnWidth - 4) + "s | %-" + (confidenceColumnWidth - 4) + ".2f%%    |", stt, "[" + antecedentStr + "] => [" + consequentStr + "]", confidence * 100);

        if (!printedHeader) {
            printedHeader = true;
            String header = String.format("| %-4s| %-" + (itemsetColumnWidth - 4) + "s | %-" + (confidenceColumnWidth - 4) + "s |", "STT", "Rule", "Confidence");
            String separator = "|-----+----------------------------------------------------------+------------|";
            return header + "\n" + separator + "\n" + line;
        } else {
            return line;
        }
    }
}
