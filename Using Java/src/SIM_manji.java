import java.io.*;
import java.util.*;


public class SIM_manji {

    public static void main(String[] args) throws IOException{
        if (args.length == 0) {
            System.out.println("Please Enter correct number of Arguments");
        }else{
           String selector = args[0];
            if (selector.equals("1")) {
//                Compressor();
            }else if (selector.equals("2")){
//                Decompressor();
            }else{
                System.out.println("Please provide the correct Input !! \n" + "1 for Compression OR 2 for Decompression");
            }
        }
    }

    static final String compressInFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\original.txt";
    static final String compressOutFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt";
    static final String decompressInFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt";
    static final String decompressOutFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\dout.txt";
    static  StringBuilder compressedCode = new StringBuilder();
    static StringBuilder decompressedCode = new StringBuilder();

    private static void Compressor() throws IOException {
        LinkedHashMap<String,Integer> dictionaryMapper = new LinkedHashMap<>();
        LinkedList<String> inputData = new LinkedList<>();
        LinkedHashMap<String,String> dictionary;
    }
    private static void Readfile(LinkedList<String> inputData, LinkedHashMap<String, Integer> dictionaryMapper) throws IOException{
        String lineRead;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(compressInFile));

        while ((lineRead = bufferedReader.readLine()) != null){
            int count;
            inputData.add(lineRead);

            if (dictionaryMapper.containsKey(lineRead)) {
                count = dictionaryMapper.get(lineRead);
                dictionaryMapper.replace(lineRead,count,count+1);
            }
            else{
                count = 1;
                dictionaryMapper.put(lineRead,count);
            }
        }
        bufferedReader.close();
    }

    private static String DictionaryGenerator (Map<String, Integer> inputMap){
        String dictionaryKey = null;
        int count = 0;
        Set<Map.Entry<String, Integer>> sorted = inputMap.entrySet();

        for (Map.Entry<String,Integer> mapEntry : sorted) {
            if(mapEntry.getValue() > count){
                count = mapEntry.getValue();
                dictionaryKey = mapEntry.getKey();
            }
        }
        return dictionaryKey;
    }

    private static LinkedHashMap <String,String> DictionaryHolder(LinkedHashMap<String,Integer> dictionaryMapper ){
        LinkedHashMap<String, String> dictionary =  new LinkedHashMap<>();
        String dictKey,dictValue;

        for(int dictIndex = 0; dictIndex < 8; dictIndex++){
            dictKey = DictionaryGenerator(dictionaryMapper);

            //Convert the dictionary index to a 3-bit binary number
            dictValue = String.format("%3s", Integer.toBinaryString(dictIndex)).replaceAll(" ", "0");
            dictionary.put(dictKey, dictValue);
            dictionaryMapper.remove(dictKey);
        }
        return dictionary;
    }

    private static void CompressMethods (LinkedList<String> inputdata, LinkedHashMap<String,String> dictionary){

        int counter =-1;
        String previousCode = "";
        String rleString;



        for (String currentCode : inputdata){
            Mismatch oneMismatchOneBit =  findMismatchLocations(dictionary,currentCode, 1);
            Mismatch twoMismatchOneBit =  findMismatchLocations(dictionary,currentCode, 2);

            String bitmaskDetailsFinder = BitmaskEncoder(dictionary,currentCode);

            List<Integer> mismatchLocations;
            String dictionaryIndex;
            if (currentCode.equals(previousCode))
                counter += 1;
            else {
                if (counter > -1) {
                    rleString = String.format("%2s", Integer.toBinaryString(counter)).replaceAll(" ", "0");
                    compressedCode.append("000").append(rleString);
                    counter = -1;
                }
                // If Code has Direct Mapping With the Dictionary Strategy
                if (dictionary.containsKey(currentCode))
                    compressedCode.append("101").append(dictionary.get(currentCode));
                    // If Code contains Mismatch of 1-bit Strategy
                else if (oneMismatchOneBit != null) {
                    int mismatchLoc;
                    String binaryLocation;
                    mismatchLocations = oneMismatchOneBit.mismatchLocations;
                    mismatchLoc = mismatchLocations.get(0);
                    binaryLocation = String.format("%5s", Integer.toBinaryString(mismatchLoc)).replaceAll(" ", "0");
                    dictionaryIndex = oneMismatchOneBit.dictionaryIndex;
                    compressedCode.append("010").append(binaryLocation).append(dictionaryIndex);
                }
                // If Code contains mismatch of two consecutive bits
                else if (twoMismatchOneBit != null && (twoMismatchOneBit.mismatchLocations.get(1) - twoMismatchOneBit.mismatchLocations.get(0)) == 1) {
                    int mismatchLoc;
                    String binaryLocation;
                    mismatchLocations = twoMismatchOneBit.mismatchLocations;
                    mismatchLoc = mismatchLocations.get(0);
                    binaryLocation = String.format("%5s", Integer.toBinaryString(mismatchLoc)).replaceAll(" ", "0");
                    dictionaryIndex = twoMismatchOneBit.dictionaryIndex;
                    compressedCode.append("011").append(binaryLocation).append(dictionaryIndex);
                }
                // If code can be compressed using bitmask compression
                else if (bitmaskDetailsFinder.length() > 7) {
                    String bitmask = bitmaskDetailsFinder.substring(0, 4);
                    int bitmaskLocation;
                    String dictIndex, binBitmaskLoc;
                    if (bitmaskDetailsFinder.length() < 9) {
                        bitmaskLocation = Integer.parseInt(String.valueOf(bitmaskDetailsFinder.charAt(4)));
                        dictIndex = bitmaskDetailsFinder.substring(5);
                    } else {
                        bitmaskLocation = Integer.parseInt(bitmaskDetailsFinder.substring(4, 6));
                        dictIndex = bitmaskDetailsFinder.substring(6);
                    }
                    binBitmaskLoc = String.format("%5s", Integer.toBinaryString(bitmaskLocation)).replaceAll(" ", "0");
                    compressedCode.append("001").append(binBitmaskLoc).append(bitmask).append(dictIndex);
                }
                // If code contains two mismatches of one bits
                else if (twoMismatchOneBit != null) {
                    int mismatchLoc1, mismatchLoc2;
                    String binaryLocation1, binaryLocation2;
                    mismatchLocations = twoMismatchOneBit.mismatchLocations;
                    mismatchLoc1 = mismatchLocations.get(0);
                    mismatchLoc2 = mismatchLocations.get(1);
                    binaryLocation1 = String.format("%5s", Integer.toBinaryString(mismatchLoc1)).replaceAll(" ", "0");
                    binaryLocation2 = String.format("%5s", Integer.toBinaryString(mismatchLoc2)).replaceAll(" ", "0");
                    dictionaryIndex = twoMismatchOneBit.dictionaryIndex;
                    compressedCode.append("100").append(binaryLocation1).append(binaryLocation2).append(dictionaryIndex);
                }
                // If Code cannot be compressed
                else
                    compressedCode.append("110").append(currentCode);
            }
            previousCode = currentCode;


        }
    }

    static class Mismatch{
        List<Integer> mismatchLocations;
        int numOfMismatch;
        String dictionaryIndex;

        Mismatch(List<Integer> misLoc, int numBitMismatch, String dictIndex){
            mismatchLocations = misLoc;
            numOfMismatch = numBitMismatch;
            dictionaryIndex = dictIndex;
        }
    }

    private static Mismatch findMismatchLocations(LinkedHashMap<String, String> dictionary, String binaryCode, int NumBitMismatches) {
        List<Integer> mismatchLocations = new ArrayList<>();
        String dictionaryIndex = "";
        int numOfMismatches = 0;

        //Find the mismatch locations with respect to each dictionary entry
        for (Map.Entry<String, String> dictionaryEntry : dictionary.entrySet()) {
            for (int index = 0; index < binaryCode.length(); index++) {
                if (dictionaryEntry.getKey().charAt(index) != binaryCode.charAt(index)) {
                    mismatchLocations.add(index);
                }
            }
            numOfMismatches = mismatchLocations.size();
            dictionaryIndex = dictionaryEntry.getValue();

            //If the number of mismatches equals the required mismatches, break the loop
            if (numOfMismatches == NumBitMismatches)
                break;
            mismatchLocations = new ArrayList<>();
            numOfMismatches = 0;
        }

        //If the required number of mismatches were found with respect to any of the dictionary entry
        //Return the mismatch locations, their values and the dictionary index
        if (numOfMismatches == NumBitMismatches)
            return new Mismatch(mismatchLocations, numOfMismatches, dictionaryIndex);
        else
            return null;
    }

    private static String BitmaskEncoder(LinkedHashMap<String, String> dictionary, String currentCode) {
        Mismatch twoBitMismatch = findMismatchLocations(dictionary, currentCode, 2);
        Mismatch threeBitMismatch = findMismatchLocations(dictionary, currentCode, 3);
        Mismatch fourBitMismatch = findMismatchLocations(dictionary, currentCode, 4);
        StringBuilder stringBuilder = new StringBuilder();

        // If the code contains two bit mismatches that are not consecutive
        if(twoBitMismatch != null){
            if (twoBitMismatch.mismatchLocations.get(1) - twoBitMismatch.mismatchLocations.get(0) < 4 &&
                    twoBitMismatch.mismatchLocations.get(1) - twoBitMismatch.mismatchLocations.get(0) > 1) {

                // If the mismatch pattern  is "MNMN" where M = mismatch and N = No mismatch
                if (twoBitMismatch.mismatchLocations.get(1) - twoBitMismatch.mismatchLocations.get(0) == 2) {
                    stringBuilder.append("1010");
                    stringBuilder.append(twoBitMismatch.mismatchLocations.get(0));
                    stringBuilder.append(twoBitMismatch.dictionaryIndex);
                }
                // If the mismatch pattern  is "MNNM" where M = mismatch and N = No mismatch
                else {
                    stringBuilder.append("1001");
                    stringBuilder.append(twoBitMismatch.mismatchLocations.get(0));
                    stringBuilder.append(twoBitMismatch.dictionaryIndex);
                }
            }
        }
        //If the code contains three bit mismatches
        else if(threeBitMismatch != null){
            if (threeBitMismatch.mismatchLocations.get(2) - threeBitMismatch.mismatchLocations.get(0) < 4) {
                // If the mismatch pattern  is "MMMN" where M = mismatch and N = No mismatch
                if(threeBitMismatch.mismatchLocations.get(2) - threeBitMismatch.mismatchLocations.get(0) == 2){
                    stringBuilder.append("1110");
                    stringBuilder.append(threeBitMismatch.mismatchLocations.get(0));
                    stringBuilder.append(threeBitMismatch.dictionaryIndex);
                }
                // If the mismatch pattern  is "MMNM" where M = mismatch and N = No mismatch
                else if(threeBitMismatch.mismatchLocations.get(2) - threeBitMismatch.mismatchLocations.get(0) == 3) {
                    stringBuilder.append("1101");
                    stringBuilder.append(threeBitMismatch.mismatchLocations.get(0));
                    stringBuilder.append(threeBitMismatch.dictionaryIndex);
                }
                // If the mismatch pattern  is "MNMM" where M = mismatch and N = No mismatch
                else if(threeBitMismatch.mismatchLocations.get(1) - threeBitMismatch.mismatchLocations.get(0) == 2){
                    stringBuilder.append("1011");
                    stringBuilder.append(threeBitMismatch.mismatchLocations.get(0));
                    stringBuilder.append(threeBitMismatch.dictionaryIndex);
                }
            }
        }
        // If the code contains four bit mismatches
        else if(fourBitMismatch != null){
            if (fourBitMismatch.mismatchLocations.get(3) - fourBitMismatch.mismatchLocations.get(0) < 4) {
                stringBuilder.append("1111");
                stringBuilder.append(fourBitMismatch.mismatchLocations.get(0));
                stringBuilder.append(fourBitMismatch.dictionaryIndex);
            }
        }
        //Return the string that contains the bitmask, mismatch location, dictionary index
        return stringBuilder.toString();
    }

    private static void WriteCompressedOutputToFile(LinkedHashMap<String, String> dictionary) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(compressOutFile));
        int offset = 0;
        int length = compressedCode.length();
        //Write the compressed code in batches of 32 characters per line
        while(length >= 32) {
            bufferedWriter.write(compressedCode.toString(), offset, 32);
            bufferedWriter.write("\n");
            offset += 32;
            length = compressedCode.substring(offset).length();
        }

        //If the last line contains less than 32 characters
        String lastString = compressedCode.substring(offset);
        if(length != 0)
            bufferedWriter.write(String.format("%1$-" + 32 + "s", lastString).replace(' ', '1'));

        bufferedWriter.write("\nxxxx\n");
        //Write the dictionary elements in the output file
        Set<Map.Entry<String, String>> dictionaryPrint = dictionary.entrySet();
        for (Map.Entry<String, String> mapEntry : dictionaryPrint)
            bufferedWriter.write(mapEntry.getKey() + "\n");

        //CLose the buffered writer
        bufferedWriter.close();
    }





}
