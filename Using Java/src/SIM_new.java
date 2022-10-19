import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.nio.file.*;

public class SIM_new {
    static LinkedHashMap<String, Integer> dictMap = new LinkedHashMap<String, Integer>();
    static LinkedHashMap<String, Integer> dictionary = new LinkedHashMap<String, Integer>();
    static LinkedHashMap<Integer, String> dictionaryReverse = new LinkedHashMap<Integer, String>();
    static HashMap<String, Integer> formatToLength = new HashMap<String, Integer>();
    static List<String> dictList = new ArrayList<String>();
    static int rle = 0;
    static String output = "";
    static int i;
    static String compressCode = "";
    static String deCompressCode = "";

    public static void main(String[] args){
        String argument = args[0];
        if(argument.equals("1")){
            compressor();
        } else if(argument.equals("2")){
            decompressor();
        } else
            System.out.println("Please enter correct argument");
    }

    public static void compressor(){
        String fileName = null;
        String inputCode;
        String prevCode = "invalid";

        fileName = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\original.txt";
        File file = new File(fileName);
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            while ((inputCode = br.readLine()) != null){
                dictList.add(inputCode);
                if(dictMap.containsKey(inputCode)){
                    dictMap.put(inputCode, dictMap.get(inputCode) + 1);
                } else{
                    dictMap.put(inputCode, 1);
                }
            }
        } catch(IOException e ){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
        // Dictionary Build /////////////////////////////
        dictionary = getTop8ByValue(dictMap);
        //  compressing starts
        for(i=0; i<dictList.size(); i++){
            String compInputCode = "";
            inputCode = dictList.get(i);

            if(inputCode.equals(prevCode)){
                rle++;
            } else{
                if(rle>1){//Check if it is RLE
                    rle();
                    rle = 0;
                    compressCode = compressCode;// + "\n";
                }
                if(dictionary.containsKey(inputCode)){
                    int dict_index = dictionary.get(inputCode);
                    compInputCode = String.format("%3s", Integer.toBinaryString(dict_index)).replace(' ', '0');
                    compressCode = compressCode + "101" + compInputCode;
                }
                // one bit mismatch
                else if(!((compInputCode = oneBitMismatchEncoding(inputCode)).equals(""))){// 1 Bit mismatch
                    compressCode = compressCode + "010" + compInputCode;
                }
                // two bit mismatch
                else if(!((compInputCode = twoBitMismatchEncoding(inputCode)).equals(""))){// 2 Bit mismatch
                    compressCode = compressCode + "011" + compInputCode;
                }
                //bitmaskendcoding
                else if(!((compInputCode = bitMaskEncoding(inputCode)).equals(""))){// Bit mask compression
                    compressCode = compressCode + "001" + compInputCode;
                }
                // two bit mismatch
                else if(!((compInputCode = twoBitAllEnconding(inputCode)).equals(""))){// 2 bit mismatch anywhere
                    compressCode = compressCode + "100" + compInputCode;
                }
                else {// Original binary
                    compressCode = compressCode + "110" + inputCode;
                }
                rle = 1;
                prevCode = inputCode;
            }
        }

        String comp = String.format("%-" + ((compressCode.length()/32)+1)*32 + "s", compressCode).replace(' ', '1');
        String parsedStr = comp.replaceAll("(.{32})", "$1\n").trim();

        // write in output file
        try{
            PrintStream fileStream = new PrintStream("D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt");
            System.setOut(fileStream);
        } catch(IOException e ){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(parsedStr);
        System.out.println("xxxx");
        Iterator it = dictionary.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey());
        }


    }
    public static void decompressor(){
        String fileName = "D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\cout.txt";
        String dict = "";
        String format = "";
        String inputCode = "";
        int instLength = 0;

        i = 0;
        // Read from the fileName
        try{
            compressCode = new String(Files.readAllBytes(Paths.get(fileName)));
        } catch(IOException e ){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
        // Implement Dictionary

        dict = compressCode.substring(compressCode.lastIndexOf("x") + 1).trim();
        String lines[] = dict.split("\\r?\\n");
        for(i=0; i<lines.length; i++){
            dictionaryReverse.put(i, lines[i]);
        }
        i=0;
        //Implement Format to Instruction length dictionary
        formatToLength.put("000", 2);
        formatToLength.put("001", 12);
        formatToLength.put("010", 8);
        formatToLength.put("011", 8);
        formatToLength.put("100", 13);
        formatToLength.put("101", 3);
        formatToLength.put("110", 32);
        compressCode = compressCode.replaceAll("\\r\\n|\\r|\\n", "");

        //Loop through compressed string to find the format
        while(compressCode.charAt(i) != 'x'){
            //Get the first 3 bits specifying whether RLE, DM, 1bit, 2bit or more
            format = getFormat();
            if(formatToLength.containsKey(format)){
                instLength = formatToLength.get(format);
            } else {
                break;
            }
            //get the next bits corresponding to that instruction
            StringBuilder sb = new StringBuilder();
            for(int j=0; j<instLength; j++){
                sb.append(compressCode.charAt(i++));
            }
            inputCode = sb.toString();
            if(format.equals("000")){
                deCompressCode = runLE(inputCode);
            } else if(format.equals("001")){
                deCompressCode = bitMaskDecoder(inputCode);
            } else if(format.equals("010")){
                deCompressCode = oneMismatchOneBit(inputCode);
            } else if(format.equals("011")){
                deCompressCode = twoMismatchOneBit(inputCode);
            } else if(format.equals("100")){
                deCompressCode = twoBitMismatchAnywhere(inputCode);
            } else if(format.equals("101")){
                deCompressCode = directMatch(inputCode);
            } else if(format.equals("110")){
                deCompressCode = original(inputCode);
            }
        }
        String parsedStr = output.replaceAll("(.{32})", "$1\n").trim();

        // write the decompressed file
        try{
            PrintStream fileStream = new PrintStream("D:\\ACA\\sem 6\\Embedded\\Codecompression\\src\\mamji.txt");
            System.setOut(fileStream);
        } catch(IOException e ){
            e.printStackTrace();
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(parsedStr);
    }

    // compressing functions
    public static String intToStr(int dict_index){
        return String.format("%3s", Integer.toBinaryString(dict_index)).replace(' ', '0');
    }

    public static LinkedHashMap<String, Integer> getTop8ByValue(LinkedHashMap<String, Integer> map) {
        AtomicInteger index = new AtomicInteger();
        return map.entrySet().stream()
                .sorted(Entry.<String, Integer> comparingByValue().reversed())
                .limit(8)
                .collect(
                        Collectors.toMap(
                                e -> e.getKey(),
                                e -> index.getAndIncrement(),
                                (k, v) -> {
                                    throw new IllegalStateException("Duplicate key " + k);
                                },
                                LinkedHashMap::new)
                );
    }

    public static void rle(){
        String compInst = "";
        switch (rle){
            case 2: compInst = "00";
                break;
            case 3: compInst = "01";
                break;
            case 4: compInst = "10";
                break;
            case 5: compInst = "11";
                break;
        }
        compressCode = compressCode + "000" + compInst;
    }

    public static String oneBitMismatchEncoding(String inputCode){
        for(int h=0; h<inputCode.length(); h++){
            StringBuilder inputCodeSb = new StringBuilder(inputCode);
            inputCodeSb.setCharAt(h, inputCodeSb.charAt(h)=='0' ? '1':'0');
            String instruction = inputCodeSb.toString();
            if(dictionary.containsKey(instruction)){
                String mismatchLocation = String.format("%5s", Integer.toBinaryString(h)).replace(' ', '0');
                int dict_index = dictionary.get(instruction);
                String dict_index_S = intToStr(dict_index);
                return mismatchLocation + dict_index_S;
            }
        }
        return "";
    }

    public static String twoBitMismatchEncoding(String inputCode){
        for(int h=0; h<inputCode.length()-1; h++){
            StringBuilder inputCodeSb = new StringBuilder(inputCode);
            if(inputCodeSb.charAt(h) == '0' && inputCodeSb.charAt(h+1) == '0'){
                inputCodeSb.setCharAt(h, '1');
                inputCodeSb.setCharAt(h+1, '1');
            } else if(inputCodeSb.charAt(h) == '0' && inputCodeSb.charAt(h+1) == '1'){
                inputCodeSb.setCharAt(h, '1');
                inputCodeSb.setCharAt(h+1, '0');
            } else if(inputCodeSb.charAt(h) == '1' && inputCodeSb.charAt(h+1) == '0'){
                inputCodeSb.setCharAt(h, '0');
                inputCodeSb.setCharAt(h+1, '1');
            } else {
                inputCodeSb.setCharAt(h, '0');
                inputCodeSb.setCharAt(h+1, '0');
            }
            String instruction = inputCodeSb.toString();
            if(dictionary.containsKey(instruction)){
                int dict_index = dictionary.get(instruction);
                String dict_index_S = intToStr(dict_index);
                String mismatchLocation = String.format("%5s", Integer.toBinaryString(h)).replace(' ', '0');
                return mismatchLocation + dict_index_S;
            }
        }
        return "";
    }

    public static String bitMaskEncoding(String inputCode){
        for(int i=8; i<16; i++){
            String maskSmall = String.format("%4s", Integer.toBinaryString(i)).replace(' ', '0');
            for(int q=0; q<29; q++){
                StringBuilder inputCodeSb = new StringBuilder();

                String padLeftZeros = String.format("%" + (q+4) + "s", maskSmall).replace(' ', '0');
                String mask = String.format("%-" + 32 + "s", padLeftZeros ).replace(' ', '0');

                for(int j=0; j<32; j++){
                    inputCodeSb.append(charOf(bitOf(mask.charAt(j)) ^ bitOf(inputCode.charAt(j))));
                }

                String instruction = inputCodeSb.toString();

                if(dictionary.containsKey(instruction)){
                    int dict_index = dictionary.get(instruction);
                    String dict_index_S = intToStr(dict_index);
                    String mismatchLocation = String.format("%5s", Integer.toBinaryString(q)).replace(' ', '0');
                    return  mismatchLocation + maskSmall + dict_index_S;
                }
            }
        }
        return "";
    }

    public static String cyclicLeftShift(String s, int k){
        k = k%s.length();
        return s.substring(k) + s.substring(0, k);
    }

    public static String twoBitAllEnconding(String inputCode){
        String maskLeftBit = "10000000000000000000000000000000";

        for(int i=0; i<31; i++){

            String leftMask = cyclicLeftShift(maskLeftBit, (32-i));
            for(int j=i+1; j<32; j++){
                StringBuilder maskSb = new StringBuilder();
                StringBuilder inputCodeSb = new StringBuilder();

                String rightMask = cyclicLeftShift(maskLeftBit, (32-j));
                for(int h=0; h<32; h++){
                    maskSb.append(charOf(bitOf(leftMask.charAt(h)) ^ bitOf(rightMask.charAt(h))));
                    inputCodeSb.append(charOf(bitOf(maskSb.charAt(h)) ^ bitOf(inputCode.charAt(h))));
                }
                String instruction = inputCodeSb.toString();

                if(dictionary.containsKey(instruction)){
                    int dict_index = dictionary.get(instruction);
                    String dict_index_S = intToStr(dict_index);
                    String leftMismatchLocation = String.format("%5s", Integer.toBinaryString(i)).replace(' ', '0');
                    String rightMismatchLocation = String.format("%5s", Integer.toBinaryString(j)).replace(' ', '0');
                    return  leftMismatchLocation +  rightMismatchLocation +  dict_index_S;
                }
            }
        }

        return "";
    }

    private static boolean bitOf(char in) {
        return (in == '1');
    }
    private static char charOf(boolean in) {
        return (in) ? '1' : '0';
    }

    // Decompressing functions
    public static String runLE(String inputCode){
        if(inputCode.equals("00")){
            output = output + deCompressCode;
        } else if(inputCode.equals("01")){
            output = output + deCompressCode + deCompressCode;
        } else if(inputCode.equals("10")){
            output = output + deCompressCode + deCompressCode + deCompressCode;
        } else if(inputCode.equals("11")){
            output = output + deCompressCode + deCompressCode + deCompressCode + deCompressCode;
        }
        return deCompressCode;
    }

    public static String bitMaskDecoder(String inputCode){
        String mismatchLocation = inputCode.substring(0,5);
        String bitmask = inputCode.substring(5,9);
        String dict_index = inputCode.substring(9, inputCode.length());
        int dict_index_I = Integer.parseInt(dict_index, 2);
        String instruction = "";
        if(dictionaryReverse.containsKey(dict_index_I)){
            instruction = dictionaryReverse.get(dict_index_I);
        }
        int mismatchLocation_I = Integer.parseInt(mismatchLocation, 2);
        StringBuilder instructionSb = new StringBuilder();

        String padLeftZeros = String.format("%" + (mismatchLocation_I+4) + "s", bitmask).replace(' ', '0');
        String mask = String.format("%-" + 32 + "s", padLeftZeros ).replace(' ', '0');
        for(int j=0; j<32; j++){
            instructionSb.append(charOf(bitOf(mask.charAt(j)) ^ bitOf(instruction.charAt(j))));
        }
        deCompressCode = instructionSb.toString();
        output = output + deCompressCode;
        return deCompressCode;
    }

    public static String oneMismatchOneBit(String inputCode){
        String mismatchLocation = inputCode.substring(0,5);
        String dict_index = inputCode.substring(5, inputCode.length());
        int dict_index_I = Integer.parseInt(dict_index, 2);
        String instruction = "";
        if(dictionaryReverse.containsKey(dict_index_I)){
            instruction = dictionaryReverse.get(dict_index_I);
        }
        int mismatchLocation_I = Integer.parseInt(mismatchLocation, 2);
        StringBuilder instructionSb = new StringBuilder(instruction);
        instructionSb.setCharAt(mismatchLocation_I, instructionSb.charAt(mismatchLocation_I)=='0' ? '1':'0');
        deCompressCode = instructionSb.toString();
        output = output + deCompressCode;

        return deCompressCode;
    }

    public static String twoMismatchOneBit(String inputCode){
        String mismatchLocation = inputCode.substring(0,5);
        String dict_index = inputCode.substring(5, inputCode.length());
        int dict_index_I = Integer.parseInt(dict_index, 2);
        String instruction = "";
        if(dictionaryReverse.containsKey(dict_index_I)){
            instruction = dictionaryReverse.get(dict_index_I);
        }
        int mismatchLocation_I = Integer.parseInt(mismatchLocation, 2);
        StringBuilder instructionSb = new StringBuilder(instruction);

        instructionSb.setCharAt(mismatchLocation_I, instructionSb.charAt(mismatchLocation_I)=='0' ? '1':'0');
        instructionSb.setCharAt(mismatchLocation_I+1, instructionSb.charAt(mismatchLocation_I+1)=='0' ? '1':'0');
        deCompressCode = instructionSb.toString();
        output = output + deCompressCode;

        return deCompressCode;
    }

    public static String twoBitMismatchAnywhere(String inputCode){
        String mismatchLocationL = inputCode.substring(0,5);
        String mismatchLocationR = inputCode.substring(5,10);
        String dict_index = inputCode.substring(10, inputCode.length());
        int dict_index_I = Integer.parseInt(dict_index, 2);
        String instruction = "";
        if(dictionaryReverse.containsKey(dict_index_I)){
            instruction = dictionaryReverse.get(dict_index_I);
        }
        int mismatchLocation_IL = Integer.parseInt(mismatchLocationL, 2);
        int mismatchLocation_IR = Integer.parseInt(mismatchLocationR, 2);
        StringBuilder instructionSb = new StringBuilder(instruction);

        instructionSb.setCharAt(mismatchLocation_IL, instructionSb.charAt(mismatchLocation_IL)=='0' ? '1':'0');
        instructionSb.setCharAt(mismatchLocation_IR, instructionSb.charAt(mismatchLocation_IR)=='0' ? '1':'0');
        deCompressCode = instructionSb.toString();
        output = output + deCompressCode;

        return deCompressCode;
    }

    public static String directMatch(String inputCode){
        String dict_index = inputCode.substring(0, inputCode.length());
        int dict_index_I = Integer.parseInt(dict_index, 2);
        if(dictionaryReverse.containsKey(dict_index_I)){
            deCompressCode = dictionaryReverse.get(dict_index_I);
        }
        output = output + deCompressCode;
        return deCompressCode;
    }

    public static String original(String inputCode){
        deCompressCode = inputCode;
        output = output + deCompressCode;
        return deCompressCode;
    }
    public static String getFormat(){
        StringBuilder format = new StringBuilder();
        for(int j=0; j<3; j++){
            format.append(compressCode.charAt(i++));
        }
        return format.toString();
    }
}
