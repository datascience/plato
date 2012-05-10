package eu.scape_project.pw.planning.manager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.planets_project.pp.plato.model.DigitalObject;
import eu.scape_project.pw.planning.utils.OS;

/**
 * A handler for loading and storing bytestreams of a (read: one) plan, it: -
 * uses a {@link IByteStreamStorage } for persisting the bytestreams permanently,
 * - and also takes care of caching bytestreams. Note: - It does NOT take any
 * measures to prevent concurrency issues,ensure thread savety. But this is ok,
 * because : - A plan cannot be loaded and accessed more than once at a time,
 * the ByteStreamManager shares the same (conversation) scope. - Beside
 * different planning-conversations we do not read/store bytestreams with
 * multiple threads.
 * 
 * @author Michael Kraxner
 * 
 */
// FIXME: use cache!
public class ByteStreamManager implements Serializable, IByteStreamManager {

    private static final long serialVersionUID = 7205715730617180554L;

    @Inject
    private Logger log;

    @Inject
    private IByteStreamStorage storage;

    private Map<String, File> tempDigitalObjects = new HashMap<String, File>();

    private File tempDir = null;

    public ByteStreamManager() {
    }

    /**
     * @see IByteStreamManager#store(DigitalObject)
     */
    @Override
    public String store(DigitalObject o) throws StorageException {
        if (o == null) {
            throw new StorageException("Tried to store digital object which is not defined (null)");
        }
        return store(o.getPid(), o.getData().getData());
    }

    /**
     * @see IByteStreamManager#load(DigitalObject)
     */
    @Override
    public byte[] load(DigitalObject o) throws StorageException {
        if (o == null) {
            throw new StorageException("Tried to load digital object which is not defined (null)");
        }
        byte[] result = load(o.getPid());
        o.getData().setData(result);
        return result;
    }

    /**
     * 
     * @see IByteStreamManager#store(String, byte[])
     */
    public String store(String pid, byte[] bytestream) throws StorageException {
        if ("".equals(pid)) {
            pid = null;
        }
        pid = storage.store(pid, bytestream);
        // we have also to update the chache!
        try {
            cacheObject(pid, bytestream);
        } catch (IOException e) {
            throw new StorageException("failed to cache object", e);
        }
        return pid;
    }

    public byte[] load(String pid) throws StorageException {
        byte[] data = storage.load(pid);
        try {
            cacheObject(pid, data);
        } catch (IOException e) {
            throw new StorageException("failed to cache object", e);
        }
        return data;
    }

    /**
     * 
     * @see IByteStreamManager#getTempFile(String)
     */
    public File getTempFile(String pid) {
        File tmp = tempDigitalObjects.get(pid);
        if (tmp == null) {
            try {
                load(pid);
                tmp = tempDigitalObjects.get(pid);
            } catch (StorageException e) {
                log.error("failed to retrieve object: " + pid, e);
                return null;
            }
        }
        return tmp;
    }

    public void delete(String pid) throws StorageException {
        if ((pid == null) || (pid.isEmpty())) {
            return;
        }
        File f = tempDigitalObjects.remove(pid);
        if (f != null) {
            f.delete();
        }
        storage.delete(pid);
    }

    /**
     * Stores the given bytestream to a tempfile and keeps the handle for later
     * use.
     * 
     * @param pid
     * @param bitstream
     */
    private void cacheObject(String pid, byte[] bitstream) throws IOException {
        String filename = pid;
        String fileExtension = "";
        int bodyEnd = filename.lastIndexOf(".");
        if (bodyEnd >= 0) {
            fileExtension = filename.substring(bodyEnd);
        }

        String tempFileName = tempDir.getAbsolutePath() + System.nanoTime() + fileExtension;
        File tempFile = new File(tempFileName);

        OutputStream fileStream;

        fileStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        fileStream.write(bitstream);
        fileStream.close();

        // queue file for deletion, in case the clean up fails
        tempFile.deleteOnExit();

        // put the temp file in the map
        tempDigitalObjects.put(pid, tempFile);
    }

    /**
     * Creates a new temp directory for this ByteStreamManager
     */
    @PostConstruct
    public void init() {
        tempDir = new File(OS.getTmpPath() + "digitalobjects" + System.nanoTime() + File.separator);
        tempDir.mkdir();
        tempDir.deleteOnExit();
    }

    /**
     * Cleanup of tempfiles, and handles to loaded digital objects
     */
    @PreDestroy
    public void destroy() {
        OS.deleteDirectory(tempDir);
        tempDigitalObjects.clear();
    }

}
