import java.io.*;
import java.util.*;

public class SIM {

    public static void main(String[] args) throws IOException {
        if(args.length == 0){
            System.out.println("Please Enter correct number of Arguments!!");
            Decompressor();
        }else {
            String selector = args[0];
            if(selector.equals("1"))
                Compressor();
            else if(selector.equals("2"))
                Decompressor();
            else
                System.out.println("Please provide the correct Input!!\n" +
                        "(1 for Compression OR 2 for  Decompression)");
        }
    }

    static final String compressInFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\original.txt";
    static final String compressOutFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt";
    static final String decompressInFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt";
    static final String decompressOutFile = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\dout.txt";
    static  StringBuilder compressedCode = new StringBuilder();
    static StringBuilder decompressedCode = new StringBuilder();

//---------------------------------------------COMPRESSION ALGORITHM----------------------------------------------------

    // Method for the compression algorithm
    private static void Compressor() throws IOException {
        LinkedHashMap<String, Integer> tempDictMap = new LinkedHashMap<>();
        LinkedList<String> inputList = new LinkedList<>();
        LinkedHashMap<String, String> dictionary;

        //Function to read the original uncompressed input file for compression
        ReadOriginalFile(inputList,tempDictMap);
        //Get the final Dictionary
        dictionary = DictionaryFunction(tempDictMap);
        //Implementation of Compression Strategies
        ImplementCompressionStrategies(inputList, dictionary);
        //Function to write the compressed file into the output file
        WriteCompressedOutputToFile(dictionary);
    }

    // Function to read the original uncompressed input file for compression
    private static void ReadOriginalFile(LinkedList<String> inputList, LinkedHashMap<String, Integer> tempDictMap) throws IOException {

        String lineReader;
        BufferedReader bufferReader = new BufferedReader(new FileReader(compressInFile));

        // Take the input from the input file and Store it in the map along with its frequency of occurrence
        while ((lineReader = bufferReader.readLine()) != null) {
            int count;
            inputList.add(lineReader);
            if(tempDictMap.containsKey(lineReader)){
                count = tempDictMap.get(lineReader);
                tempDictMap.replace(lineReader, count, count+1);
            }
            else {
                count = 1;
                tempDictMap.put(lineReader, count);
            }
        }

        //Close the buffered reader
        bufferReader.close();
    }

    // Function to create an 8-entries dictionary with the keys having the highest frequencies
    private static String DictionaryMaker(Map<String, Integer> inputMap) {
        String dictKey = null;
        int count = 0;
        Set<Map.Entry<String, Integer>> sorted = inputMap.entrySet();
        //Fetch the highest frequency code from the input map
        for (Map.Entry<String, Integer> mapEntry : sorted)
            if (mapEntry.getValue() > count) {
                count = mapEntry.getValue();
                dictKey = mapEntry.getKey();
            }
        return dictKey;
    }

    //Function to create a Map for Dictionary with its values mapped to the respective dictionary index
    private static LinkedHashMap<String, String> DictionaryFunction(LinkedHashMap<String, Integer> tempDictMap) {
        LinkedHashMap<String, String> dictionary = new LinkedHashMap<>();
        String dictKey, dictValue;
        for(int dictIndex = 0; dictIndex < 8; dictIndex++){
            dictKey = DictionaryMaker(tempDictMap);
            //Convert the dictionary index to a 3-bit binary number
            dictValue = String.format("%3s", Integer.toBinaryString(dictIndex)).replaceAll(" ", "0");
            dictionary.put(dictKey, dictValue);
            tempDictMap.remove(dictKey);
        }
        return dictionary;
    }

    // Function to implement all the compression strategies
    private static void ImplementCompressionStrategies(LinkedList<String> inputList, LinkedHashMap<String, String> dictionary) {

        int rleCounter = -1;
        String previousCode = "";
        String rleString;

        for (String currentCode : inputList) {
            //Run Length Encoding Strategy
            Mismatch oneMismatchOneBit = findMismatchLocations(dictionary, currentCode, 1);
            Mismatch twoMismatchOneBit = findMismatchLocations(dictionary, currentCode, 2);
            String bitmaskDetailsFinder = BitmaskEncoder(dictionary, currentCode);
            List<Integer> mismatchLocations;
            String dictionaryIndex;
            if (currentCode.equals(previousCode))
                rleCounter += 1;
            else {
                if (rleCounter > -1) {
                    rleString = String.format("%2s", Integer.toBinaryString(rleCounter)).replaceAll(" ", "0");
                    compressedCode.append("000").append(rleString);
                    rleCounter = -1;
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

    //Separate class for finding all the mismatches
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

    //Function to find the number of mismatches, location of the mismatches and the bit that is mismatched
    //Along with the concerned Dictionary Index
    private static Mismatch findMismatchLocations(LinkedHashMap<String, String> dictionary, String binaryCode, int NumBitMismatches) {
        List<Integer> mismatchLocations = new ArrayList<>();
        String dictionaryIndex = "";
        int numOfMismatches = 0;

        //Find the mismatch locations with respect to each dictionary entry
        for (Map.Entry<String, String> dictionaryEntry : dictionary.entrySet()) {
            for(int index = 0; index < binaryCode.length(); index++){
                if(dictionaryEntry.getKey().charAt(index) != binaryCode.charAt(index)){
                    mismatchLocations.add(index);
                }
            }
            numOfMismatches = mismatchLocations.size();
            dictionaryIndex = dictionaryEntry.getValue();

            //If the number of mismatches equals the required mismatches, break the loop
            if(numOfMismatches == NumBitMismatches)
                break;
            mismatchLocations = new ArrayList<>();
            numOfMismatches = 0;
        }

        //If the required number of mismatches were found with respect to any of the dictionary entry
        //Return the mismatch locations, their values and the dictionary index
        if(numOfMismatches == NumBitMismatches)
            return new Mismatch(mismatchLocations, numOfMismatches, dictionaryIndex);
        else
            return null;
    }

    //Function to create the BitMask for bitmask encoding
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

    //Code to write the output of the compressed code to cout.txt file
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

//--------------------------------------------DECOMPRESSION ALGORITHM---------------------------------------------------

    //Method for the decompression algorithm
    private static void Decompressor() throws IOException {
        String lineReader, dictValue;
        int dictIndex = 0, offset = 0;

        BufferedReader bufferReader = new BufferedReader(new FileReader(decompressInFile));
        StringBuilder compressedCode = new StringBuilder();
        LinkedHashMap<String,String> Dictionary = new LinkedHashMap<>();

        //Read the compressed code and store it in compressedCode String
        while ((lineReader = bufferReader.readLine()) != null){
            if(lineReader.equals("xxxx"))
                break;
            compressedCode.append(lineReader);
        }
        //Read the entries of the dictionary and store it in a dictionary that is a linked hashMap
        while ((lineReader = bufferReader.readLine()) != null){
            dictValue = String.format("%3s", Integer.toBinaryString(dictIndex)).replaceAll(" ", "0");
            Dictionary.put(dictValue,lineReader);
            dictIndex++;
        }

        //Decode the compressed code by breaking it into bits and pieces
        while(offset != compressedCode.length())
            offset = DecodingStrategySelector(compressedCode, Dictionary, offset);

        //Write the decompressed Output to File
        WriteDecompressedOutputToFile();
    }

    private static int DecodingStrategySelector(StringBuilder compressedCode, LinkedHashMap<String, String> Dictionary, int offset) {
        switch (compressedCode.substring(offset, offset + 3)) {
            //The case when the code is compressed using RLE
            case "000":
                RLEDecoding(compressedCode, offset);
                offset += 5;
                break;
            //The case when the code is compressed using Bitmask compression
            case "001":
                BitMaskDecoding(compressedCode, offset, Dictionary);
                offset += 15;
                break;
            //The case when the code has one bit mismatch with a dictionary entry
            case "010":
                OneBitMismatchDecoding(compressedCode, offset, Dictionary);
                offset += 11;
                break;
            //The case when the code has two bits mismatch with a dictionary entry
            case "011":
                TwoBitMismatchDecoding(compressedCode, offset, Dictionary);
                offset += 11;
                break;
            //The case when the code has two one bit mismatches with a dictionary entry
            case "100":
                Two_OneBitMismatchDecoding(compressedCode, offset, Dictionary);
                offset += 16;
                break;
            //The case when the code has a direct mapping with the dictionary
            case "101":
                DirectDictionaryMappingDecoding(compressedCode, offset, Dictionary);
                offset += 6;
                break;
            //The case when the code is uncompressed
            case "110":
                OriginalBinaryDecoding(compressedCode, offset);
                offset += 35;
                break;
            //WHen the last bits are padded with 1's
            case "111":
                offset = compressedCode.length();
                break;
        }
        return offset;
    }

    // Decoding strategy when the code was compressed using RLE
    private static void RLEDecoding(StringBuilder compressedCode, int offset) {
        String compCode = compressedCode.substring(offset+3, offset+5);
        int rleCount = Integer.parseInt(compCode,2) + 1;

        //fetch the previous code
        String previousCode = decompressedCode.substring(decompressedCode.length() - 33);
        //Add as many lines of previous code as the RLE count
        decompressedCode.append(String.valueOf(previousCode).repeat(Math.max(0, rleCount)));
    }

    // Decoding strategy when the code was compressed using RLE
    private static void BitMaskDecoding(StringBuilder compressedCode, int offset, LinkedHashMap<String, String> dictionary) {
        String bitmaskLocation, bitMask, dictIndex, dictionaryEntry;

        //fetch the details of the bitmask
        bitmaskLocation = compressedCode.substring(offset + 3, offset + 8);
        int bitmaskLoc = Integer.parseInt(bitmaskLocation, 2);
        bitMask = compressedCode.substring(offset + 8, offset + 12);

        dictIndex = compressedCode.substring(offset + 12, offset + 15);
        dictionaryEntry = dictionary.get(dictIndex);

        //Correct  the mismatch according to the bitmask
        for(int bitPosition = 0; bitPosition < 4; bitPosition ++){
            if(bitMask.charAt(bitPosition) == '1')
                dictionaryEntry = mismatchCorrector(dictionaryEntry, bitmaskLoc + bitPosition);
        }

        decompressedCode.append(dictionaryEntry).append("\n");
    }

    //Decoding strategy when there is only one bit mismatch with a dictionary entry
    private static void OneBitMismatchDecoding(StringBuilder compressedCode, int offset, LinkedHashMap<String, String> dictionary) {
        String dictionaryEntry, mismatchLocation, dictionaryIndex;

        dictionaryIndex = compressedCode.substring(offset + 8, offset + 11);
        dictionaryEntry = dictionary.get(dictionaryIndex);

        mismatchLocation = compressedCode.substring(offset + 3, offset + 8);
        int mismatchLoc = Integer.parseInt(mismatchLocation, 2);

        //Correct the 1-bit mismatch
        dictionaryEntry = mismatchCorrector(dictionaryEntry, mismatchLoc);

        decompressedCode.append(dictionaryEntry).append("\n");
    }

    //Decoding strategy when there are two consecutive bit mismatches with a dictionary entry
    private static void TwoBitMismatchDecoding(StringBuilder compressedCode, int offset, LinkedHashMap<String, String> dictionary) {
        String dictionaryEntry, mismatchLocation, dictionaryIndex;

        dictionaryIndex = compressedCode.substring(offset + 8, offset + 11);
        dictionaryEntry = dictionary.get(dictionaryIndex);

        mismatchLocation = compressedCode.substring(offset + 3, offset + 8);
        int mismatchLoc = Integer.parseInt(mismatchLocation, 2);

        //Correct the first mismatch bit
        dictionaryEntry = mismatchCorrector(dictionaryEntry, mismatchLoc);

        //Correct the second mismatch bit
        mismatchLoc += 1;
        dictionaryEntry = mismatchCorrector(dictionaryEntry, mismatchLoc);

        decompressedCode.append(dictionaryEntry).append("\n");
    }

    ////Decoding strategy when there are two 1-bit mismatches with a dictionary entry
    private static void Two_OneBitMismatchDecoding(StringBuilder compressedCode, int offset, LinkedHashMap<String, String> dictionary) {
        String dictionaryEntry, dictionaryIndex;
        String mismatchLocation1, mismatchLocation2;

        dictionaryIndex = compressedCode.substring(offset + 13, offset + 16);
        dictionaryEntry = dictionary.get(dictionaryIndex);

        mismatchLocation1 = compressedCode.substring(offset + 3, offset + 8);
        mismatchLocation2 = compressedCode.substring(offset + 8, offset + 13);

        int mismatchLoc1 = Integer.parseInt(mismatchLocation1, 2);
        int mismatchLoc2 = Integer.parseInt(mismatchLocation2, 2);

        //Correct the two consecutive bits of mismatches
        dictionaryEntry = mismatchCorrector(dictionaryEntry, mismatchLoc1);
        dictionaryEntry = mismatchCorrector(dictionaryEntry, mismatchLoc2);

        decompressedCode.append(dictionaryEntry).append("\n");
    }

    //Function to correct the mismatch in the code with the dictionary entry at the given location
    private static String mismatchCorrector(String dictionaryEntry, int mismatchLoc) {
        if(dictionaryEntry.charAt(mismatchLoc) == '0')
            dictionaryEntry = dictionaryEntry.substring(0, mismatchLoc) + '1'
                    + dictionaryEntry.substring(mismatchLoc + 1);
        else
            dictionaryEntry = dictionaryEntry.substring(0, mismatchLoc) + '0'
                    + dictionaryEntry.substring(mismatchLoc + 1);

        return dictionaryEntry;
    }

    //Decoding strategy when there no mismatch with a dictionary entry
    private static void DirectDictionaryMappingDecoding(StringBuilder compressedCode, int offset, LinkedHashMap<String, String> dictionary) {
        String dictionaryIndex, dictionaryEntry;

        dictionaryIndex = compressedCode.substring(offset + 3, offset + 6);
        dictionaryEntry = dictionary.get(dictionaryIndex);

        decompressedCode.append(dictionaryEntry).append("\n");
    }

    //Decoding strategy when the code was not compressed
    private static void OriginalBinaryDecoding(StringBuilder compressedCode, int offset) {
        String originalBinary;
        originalBinary = compressedCode.substring(offset + 3, offset + 35);

        decompressedCode.append(originalBinary).append("\n");
    }

    //Function to write the decompressed output to the dout file
    private static void WriteDecompressedOutputToFile() throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(decompressOutFile));
        bufferedWriter.write(decompressedCode.substring(0, decompressedCode.length() - 1));
        bufferedWriter.close();
    }
}
