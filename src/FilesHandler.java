import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 *
 * Public Class {@code FilesHandler} that organizes and executes all I/O and File operations in Memory. <p></p> Uses buffer in RAM
 * that flushes after creation to limit I/O times. <p> All files are serialized to .dat files. All data paths lead to {@code src/resources} directory,
 * and raw data that is used is accepted in csv format.
 * <p><p>
 * {@code datafile}: The serialized Records data from Raw CSV file format TO bytes in .dat <p>
 * {@code indexfile}: The serialized classes and objects of the R*Tree that points to blocks in the datafile
 *
 */


class FilesHandler {
    /** Path to the Raw CSV data file in the project */
    private static final String PATH_TO_CSV = "src/resources/data.csv";

    /** Path to the DataFile in the project*/
    static final String PATH_TO_DATAFILE = "src/resources/datafile.dat";

    /** Path to the Indexfile in the project */
    static final String PATH_TO_INDEXFILE = "src/resources/indexfile.dat"; //

    /** Block Size in Memory = 32KB*/
    private static final int BLOCK_SIZE = 32 * 1024;

    /** User given data Dimensions (given csv file is 2 dimensional) */
    private static int dataDimensions;

    /** The total blocks in the datafile, written in metadata block 0 */
    private static int totalBlocksInDataFile;

    /** The total Blocks in the Indexfile, written in the MetaData Block 0 */
    private static int totalBlocksInIndexFile; //

    /** The total levels of the tree , written in the MetaData Block 0.*/
    private static int totalLevelsOfTreeIndex;

    /** Index Buffer of BlockId's and IndexBlocks.*/
    static Map<Long, IndexBlock> indexBuffer = new LinkedHashMap<>();

    /** The current {@link IndexBlock} that is being written on*/
    static IndexBlock currentIndexBlock = new IndexBlock();

    /** Writing starts in block 1, block 0 is metadata */
    static long currentBlockId = 1;


    /**
     * Getter for the CSV filepath.
     *
     * @return CSV filepath
     */


    static String getPathToCsv() {
        return PATH_TO_CSV;
    }


    /**
     * Getter for the data dimensions
     *
     * @return The data dimensions
     */


    static int getDataDimensions() {
        return dataDimensions;
    }


    /**
     * {@code serialize} method that serializes any objects into a {@code ByteArray} using Java's {@link ByteArrayOutputStream}. Used to keep
     * data in blocks of 32KB
     *
     * @param obj An {@link Object}
     * @return The {@link Object}'s {@code ByteArray}
     * @throws IOException to catch any IOException errors
     */


    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        return baos.toByteArray();
    }


    /**
     * Getter for the indexFile metadata block
     *
     * @return {@link ArrayList} of {@link Integer} describing the index file, in order:<p>
     * [{@code dataDimensions}, {@code BLOCK_SIZE}, {@code totalBlocksInIndexFile}, {@code totalLevelsOfTreeIndex}]
     */


    static ArrayList<Integer> getIndexMetaData() {
        return readMetaDataBlock(PATH_TO_INDEXFILE);
    }


    /**
     * Getter for the data file metadata block
     *
     * @return {@link ArrayList} of {@link Integer} describing the datafile, in order:
     * <p>[{@code dataDimensions}, {@code BLOCK_SIZE}, {@code totalBlocksInDataFile}]
     */


    static ArrayList<Integer> getDataMetaData() {
        return readMetaDataBlock(PATH_TO_DATAFILE);
    }


    /**
     * {@code readMetaDataBlock} method reads a file's metadata block
     *
     * @param pathToFile The filepath of the file to be read.
     * @return {@link ArrayList} of {@link Integer} describing the file.
     */


    private static ArrayList<Integer> readMetaDataBlock(String pathToFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "r");
            byte[] block = new byte[BLOCK_SIZE];
            raf.seek(0);
            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE) {
                throw new IOException("Could not read full metadata block (expected " + BLOCK_SIZE + ", got " + bytesRead + ")");
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);
            int metaDataSize = (Integer) ois.readObject();
            byte[] metadataBytes = new byte[metaDataSize];
            int actuallyRead = bais.read(metadataBytes);
            if (actuallyRead != metaDataSize) {
                throw new IOException("Could not read full metadata content");
            }
            ObjectInputStream metadataStream = new ObjectInputStream(new ByteArrayInputStream(metadataBytes));
            return (ArrayList<Integer>) metadataStream.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * {@code updateMetaDataBlock} method that updates the given file's metadata block with the class's static fields.
     *
     * @param pathToFile The filepath of the file to be updated.
     */


    private static void updateMetaDataBlock(String pathToFile) {
        try {
            ArrayList<Integer> fileMetaData = new ArrayList<>();
            fileMetaData.add(dataDimensions);
            fileMetaData.add(BLOCK_SIZE);
            if (pathToFile.equals(PATH_TO_DATAFILE)) {
                fileMetaData.add(totalBlocksInDataFile);
            } else if (pathToFile.equals(PATH_TO_INDEXFILE)) {
                fileMetaData.add(totalBlocksInIndexFile);
                fileMetaData.add(totalLevelsOfTreeIndex);
            }
            byte[] metaDataInBytes = serialize(fileMetaData);
            byte[] metaDataSizeBytes = serialize(metaDataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(metaDataSizeBytes, 0, block, 0, metaDataSizeBytes.length);
            System.arraycopy(metaDataInBytes, 0, block, metaDataSizeBytes.length, metaDataInBytes.length);
            RandomAccessFile raf = new RandomAccessFile(new File(pathToFile), "rw");
            raf.write(block);
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    //
    //
    //-------------------------------------------DATAFILE METHODS-------------------------------------------------------
    //
    //
    //


    /**
     * Getter for the total number of blocks in the Datafile
     *
     * @return The number of blocks in the datafile
     */


    static int getTotalBlocksInDataFile() {
        return totalBlocksInDataFile;
    }


    /**
     * Calculates the maximum amount of records that fit in a block of 32KB
     *
     * @return The maximum amount of records that fit in a block.
     */

    private static int calculateMaxRecordsInBlock() {
        ArrayList<Record> blockRecords = new ArrayList<>();
        int i;
        for (i = 0; i < 10000; i++) {
            ArrayList<Double> coords = new ArrayList<>();
            for (int d = 0; d < dataDimensions; d++)
                coords.add(0.0);
            Record record = new Record(0, "default_name", coords);
            blockRecords.add(record);
            byte[] recordInBytes = serializeOrEmpty(blockRecords);
            byte[] lengthInBytes = serializeOrEmpty(recordInBytes.length);
            if (lengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
                break;
        }
        return i - 1;
    }


    /**
     * Helper method for {@link #calculateMaxRecordsInBlock} that returns a serialized object OR if unable to serialize, an empty
     * byte array
     *
     * @param obj An {@link Object} to be serialized.
     * @return A byte array.
     */


    private static byte[] serializeOrEmpty(Object obj) {
        try {
            return serialize(obj);
        } catch (IOException e) {
            return new byte[0];
        }
    }


    /**
     * {@code writeDataFileBlock} method that writes a serialized block in the datafile in memory using {@link BufferedOutputStream}
     *
     * @param records The records to be serialized into a block
     */


    public static void writeDataFileBlock(ArrayList<Record> records) {
        try {
            byte[] recordInBytes = serialize(records);
            byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE) {
                throw new IllegalStateException("Block too large to fit in one data block");
            }
            System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
            System.arraycopy(recordInBytes, 0, block, metaDataLengthInBytes.length, recordInBytes.length);
            FileOutputStream fos = new FileOutputStream(PATH_TO_DATAFILE, true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(block);
            totalBlocksInDataFile++;
            updateMetaDataBlock(PATH_TO_DATAFILE);
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * {@code readDataFileBlock} method that reads a serialized block of {@link Record}, using the blockId as offset to position the
     * block in the datafile
     *
     * @param blockID The blockId offset
     * @return {@link ArrayList} of deserialized {@link Record}.
     */


    static ArrayList<Record> readDataFileBlock(long blockID) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(PATH_TO_DATAFILE), "r");
            raf.seek(blockID * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            int bytesRead = raf.read(block);
            if (bytesRead != BLOCK_SIZE)
                throw new IOException("Block size read was not " + BLOCK_SIZE + " bytes");
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            ObjectInputStream ois = new ObjectInputStream(bais);
            int recordDataLength = (Integer) ois.readObject();
            byte[] recordBytes = new byte[recordDataLength];
            int actuallyRead = bais.read(recordBytes);
            if (actuallyRead != recordDataLength)
                throw new IOException("Could not read full record data");
            ObjectInputStream recordOis = new ObjectInputStream(new ByteArrayInputStream(recordBytes));
            return (ArrayList<Record>) recordOis.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * {@code initializeDataFile} method that depending on user input, reads the existing datafile's metadata, OR creates a new
     * datafile by reading the Raw CSV data.
     *
     *
     * @param dataDimensions User inputted dimensions.
     * @param newDataFile Whether to create a new datafile or not
     */


    static void initializeDataFile(int dataDimensions, boolean newDataFile) {
        try {
            if (!newDataFile && Files.exists(Paths.get(PATH_TO_DATAFILE))) {
                ArrayList<Integer> dataFileMetaData = readMetaDataBlock(PATH_TO_DATAFILE);
                if (dataFileMetaData == null)
                    throw new Exception("Could not read datafile's MetaData block");
                FilesHandler.dataDimensions = dataFileMetaData.get(0);
                totalBlocksInDataFile = dataFileMetaData.get(2);
            } else {
                Files.deleteIfExists(Paths.get(PATH_TO_DATAFILE));
                FilesHandler.dataDimensions = dataDimensions;
                totalBlocksInDataFile = 1;
                updateMetaDataBlock(PATH_TO_DATAFILE);
                ArrayList<Record> blockRecords = new ArrayList<>();
                BufferedReader csvReader = new BufferedReader(new FileReader(PATH_TO_CSV));
                csvReader.readLine();
                int maxRecordsInBlock = calculateMaxRecordsInBlock();
                String line;
                int currentSize = 0;

                while ((line = csvReader.readLine()) != null) {
                    try {
                        Record r = new Record(line);
                        byte[] serialized = serialize(r);  // Αν δεν έχεις serialize(Record), μπορώ να σου τη δώσω

                        if (serialized.length > BLOCK_SIZE - 100) {
                            System.out.println("Skipping oversized record: " + r.getRecordID());
                            continue;
                        }

                        if (currentSize + serialized.length > BLOCK_SIZE) {
                            writeDataFileBlock(blockRecords);
                            blockRecords.clear();
                            currentSize = 0;
                        }

                        blockRecords.add(r);
                        currentSize += serialized.length;

                    } catch (Exception ex) {
                        System.out.println("Skipping malformed record: " + line);
                    }


                }

                csvReader.close();
                if (!blockRecords.isEmpty())
                    writeDataFileBlock(blockRecords);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    //
    //
    //-------------------------------------------INDEXFILE METHODS------------------------------------------------------
    //
    //
    //


    /**
     * Getter for the total number of blocks in the index File
     *
     * @return Total blocks in the index file
     */


    static int getTotalBlocksInIndexFile() {
        return totalBlocksInIndexFile;
    }


    /**
     * Getter for the total levels of the R*Tree index
     *
     * @return The total levels of the tree index
     */


    static int getTotalLevelsFile() {
        return totalLevelsOfTreeIndex;
    }


    /**
     * Setter of the total levels of the R*Tree index
     * @param totalLevelsOfTreeIndex The new total levels to be set
     */


    static void setLevelsOfTreeIndex(int totalLevelsOfTreeIndex) {
        FilesHandler.totalLevelsOfTreeIndex = totalLevelsOfTreeIndex;
        updateMetaDataBlock(PATH_TO_INDEXFILE);
    }


    /**
     * {@code initializeIndexFile} method that either reads the metadata of the existing indexfile OR creates a new indexfile
     * and initializes new metadata.
     *
     * @param dataDimensions User inputted data dimensions
     * @param newFile Whether to create new file or not
     */


    static void initializeIndexFile(int dataDimensions, boolean newFile) {
        try {
            if (!newFile && Files.exists(Paths.get(PATH_TO_INDEXFILE))) {
                ArrayList<Integer> indexFileMetaData = readMetaDataBlock(PATH_TO_INDEXFILE);
                FilesHandler.dataDimensions = indexFileMetaData.get(0);
                totalBlocksInIndexFile = indexFileMetaData.get(2);
                totalLevelsOfTreeIndex = indexFileMetaData.get(3);
            } else {
                Files.deleteIfExists(Paths.get(PATH_TO_INDEXFILE));
                FilesHandler.dataDimensions = dataDimensions;
                totalLevelsOfTreeIndex = 1;
                totalBlocksInIndexFile = 1;
                updateMetaDataBlock(PATH_TO_INDEXFILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * {@code writeNewIndexFileBlock} method checks whether the current {@link IndexBlock} has enough space to write a {@link Node},
     * and adds it to the {@code indexBuffer}, or
     * if the new node doesn't fit in the block, it creates a new {@link IndexBlock} in the {@code indexBuffer}, and adds the node
     * inside it
     *
     * @param node The {@link Node} to be added to the {@code indexBuffer}
     */


    static void writeNewIndexFileBlock(Node node) {
        if (!currentIndexBlock.hasSpace()) {
            indexBuffer.put(currentBlockId, currentIndexBlock);
            currentBlockId++;
            currentIndexBlock = new IndexBlock();
            totalBlocksInIndexFile++;
        }
        node.setNodeBlockId(currentBlockId);
        node.setNodeIndexInBlock(currentIndexBlock.getNodes().size());
        currentIndexBlock.addNode(node);
        indexBuffer.put(currentBlockId, currentIndexBlock);
    }


    /**
     * {@code updateIndexFileBlock} updates an {@link IndexBlock}'s {@link Node} in the {@code indexBuffer} with new data.
     * Firstly checks whether the block exists in the buffer
     * and if not, it reads directly from the indexfile.
     *
     * @param node The node to be updated.
     * @param totalLevelsOfTreeIndex unused from earlier version
     */


    static void updateIndexFileBlock(Node node, int totalLevelsOfTreeIndex) {
        long blockId = node.getNodeBlockId();
        int nodeIndex = node.getNodeIndexInBlock();

        IndexBlock indexBlock = indexBuffer.get(blockId);
        if (indexBlock == null) {
            indexBlock = readIndexFileBlock(blockId);
            if (indexBlock == null) {
                throw new IllegalStateException("Could not read IndexBlock with ID: " + blockId);
            }
        }

        // Αντικαθιστούμε το node στο σωστό index
        indexBlock.getNodes().set(nodeIndex, node);
        indexBlock.addNode(node);
        indexBuffer.put(blockId, indexBlock);
    }


    /**
     * {@code readIndexFileBlock} reads an {@link IndexBlock} directly from the indexfile. Rarely used to prevent big I/O times.
     *
     * @param blockId The block Id to be used as offset
     * @return The {@link IndexBlock} in the indexfile
     */

    static IndexBlock readIndexFileBlock(long blockId) {
        try (RandomAccessFile raf = new RandomAccessFile(PATH_TO_INDEXFILE, "r")) {
            raf.seek(blockId * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            if (raf.read(block) != BLOCK_SIZE) throw new IOException();
            ByteArrayInputStream bais = new ByteArrayInputStream(block);
            byte[] lenBytes = new byte[4];
            if (bais.read(lenBytes) != 4) throw new IOException();
            int blockLength = ByteBuffer.wrap(lenBytes).getInt();
            byte[] blockBytes = new byte[blockLength];
            if (bais.read(blockBytes) != blockLength) throw new IOException();
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(blockBytes));
            return (IndexBlock) ois.readObject();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }


    /**
     * {@code flushIndexBufferToDisk} flushes the {@code indexBuffer} to disk memory after every {@link IndexBlock} is added to the buffer.
     * The IndexBuffer {@link Map}
     * is serialized in bytes all at once, instead of every {@link IndexBlock} to be serialized every time.
     * This method drastically reduces I/O times.
     *
     */


    static void flushIndexBufferToDisk() {
        try (RandomAccessFile raf = new RandomAccessFile(PATH_TO_INDEXFILE, "rw")) {
            for (Map.Entry<Long, IndexBlock> entry : indexBuffer.entrySet()) {
                long blockId = entry.getKey();
                IndexBlock block = entry.getValue();
                byte[] blockInBytes = serialize(block);

                if (blockInBytes.length + 4 > BLOCK_SIZE) {
                    System.out.println("❌ Index block too large: " + blockInBytes.length + " bytes. Skipping block " + blockId);
                    continue;
                }


                byte[] lenBytes = ByteBuffer.allocate(4).putInt(blockInBytes.length).array();
                byte[] fileBlock = new byte[BLOCK_SIZE];
                System.arraycopy(lenBytes, 0, fileBlock, 0, 4);
                System.arraycopy(blockInBytes, 0, fileBlock, 4, blockInBytes.length);
                raf.seek(blockId * BLOCK_SIZE);
                raf.write(fileBlock);
            }
        } catch (Exception e) { e.printStackTrace(); }
        indexBuffer.clear();
    }


    /**
     * {@code appendRecordToDataBlock} is used in single {@link Record} inserts
     * <p></p>Appends a {@link Record} to a datafile block if there is enough space, if not enough space,
     * creates a new block in the datafile.
     *
     * @param record The {@link Record} to be added
     * @return The blockId where the {@link Record} was saved
     * @throws IOException to catch any IOException errors
     */


    public static long appendRecordToDataBlock(Record record) throws IOException {
        int maxRecords = calculateMaxRecordsInBlock();
        long lastBlockId = getTotalBlocksInDataFile() - 1; // last block in the datafile

        if (lastBlockId >= 1) {
            ArrayList<Record> blockRecords = readDataFileBlock(lastBlockId);
            if (blockRecords != null) {
                blockRecords.add(record);

                if (canSerializeBlock(blockRecords) && blockRecords.size() <= maxRecords) {
                    overwriteDataFileBlock(lastBlockId, blockRecords);
                    return lastBlockId;
                }
            }
        }

        // No space in existing blocks => new block
        System.out.println("Data block is full, creating new data block...\n\n");
        ArrayList<Record> newBlock = new ArrayList<>();
        newBlock.add(record);
        writeDataFileBlock(newBlock);
        return getTotalBlocksInDataFile() - 1;
    }


    /**
     * {@code canSerializeBlock} helper method that checks whether the block with the new {@link Record} can be serialized.
     *
     * @param records {@link ArrayList} of {@link Record} to be written in the block
     * @return Whether the block can be serialized or not
     * @throws IOException to catch any IOException errors
     */


    private static boolean canSerializeBlock(ArrayList<Record> records) throws IOException {
        byte[] recordInBytes = serialize(records);
        byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
        return (metaDataLengthInBytes.length + recordInBytes.length) <= BLOCK_SIZE;
    }


    /**
     * {@code overwriteDataFileBlock} overwrites a Block in the datafile. using the blockId as offset, it locates the block and reserializes it
     * with the new {@link ArrayList} of {@link Record}
     *
     * @param blockId The block Id used as offset
     * @param records {@link ArrayList} of new {@link Record} to be serialized
     * @throws IOException to catch any IOException errors
     */


    public static void overwriteDataFileBlock(long blockId, ArrayList<Record> records) throws IOException {
        byte[] recordInBytes = serialize(records);
        byte[] metaDataLengthInBytes = serialize(recordInBytes.length);
        byte[] block = new byte[BLOCK_SIZE];

        if (metaDataLengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
            throw new IllegalStateException("Block too large to overwrite");

        System.arraycopy(metaDataLengthInBytes, 0, block, 0, metaDataLengthInBytes.length);
        System.arraycopy(recordInBytes, 0, block, metaDataLengthInBytes.length, recordInBytes.length);

        try (RandomAccessFile raf = new RandomAccessFile(PATH_TO_DATAFILE, "rw")) {
            raf.seek(blockId * BLOCK_SIZE);
            raf.write(block);
        }
    }


    /**
     * {@code readNode} reads a {@link Node} using {@code blockIndex} and {@code nodeIndex} to find its position in the {@code indexBuffer}.
     * If the {@link Node} is NOT in the {@code indexBuffer},
     * it is read directly through the IndexFile with {@link #readIndexFileBlock}
     *
     * @param blockId The Node's Block id
     * @param nodeIndex The Node's Index in the Block
     * @return The Node in the indexBlock
     */


    static Node readNode(long blockId, int nodeIndex) {
        IndexBlock block = indexBuffer.get(blockId);
        if (block == null) {
            block = readIndexFileBlock(blockId);
        }
        if (block == null) {
            throw new IllegalStateException("Node-block is null");
        }
        return block.getNodes().get(nodeIndex);
    }

}

