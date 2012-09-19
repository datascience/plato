package eu.scape_project.planning.xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import eu.scape_project.planning.model.FormatInfo;
import eu.scape_project.planning.model.SampleObject;
import eu.scape_project.planning.utils.ParserException;

/**
 * A simple parser for the c3po profile.
 * 
 * @author Petar Petrov - <me@petarpetrov.org>
 * 
 */
public class C3POProfileParser {

    /**
     * A template for the description of the partition.
     */
    private static final String TYPE_OF_OBJECTS_BEGIN = "The collection consists of {1}% '{2}' files. ";

    /**
     * A template for the second most prominent type of objects in the
     * partition.
     */
    private static final String TYPE_OF_OBJECTS_SECOND = "It also contains {1}% '{2}' files. ";

    /**
     * A template for the conflicting objects in the partition.
     */
    private static final String TYPE_OF_OBJECTS_CONFLICTS = "{1}% files have conflicts. ";

    /**
     * A template for the unknown objects in the partition.
     */
    private static final String TYPE_OF_OBJECTS_UNKNOWN = "{1}% files have an unknown format. ";

    /**
     * A constant if the format distribution is missing.
     */
    private static final String MISSING = "No format distribution provided";

    /**
     * A template for the 'description of objects' field.
     */
    private static final String DESCRIPTION_OF_SAMPLES_TEMPLATE = "The sample objects were chosen by c3po using the {1} algorithm.";

    private static final Logger log = LoggerFactory.getLogger(C3POProfileParser.class);

    private Document profile;

    /**
     * Reads the profile out of the input stream and validates if needed. If the
     * document is faulty for some reason and exception will be thrown.
     * 
     * @param stream
     *            the stream of the c3po xml profile to read.
     * @param validate
     *            whether or not to validate the xml.
     * @throws ParserException
     *             if some error occurrs.
     */
    public void read(final InputStream stream, boolean validate) throws ParserException {
        ValidatingParserFactory vpf = new ValidatingParserFactory();
        SAXParser parser = null;
        try {
            parser = vpf.getValidatingParser();
        } catch (ParserConfigurationException e) {
            log.error("An error occurred while parsing the c3po profile: {}", e.getMessage());
        } catch (SAXException e) {
            log.error("An error occurred while parsing the c3po profile: {}", e.getMessage());
        }

        if (validate && !this.isValid(parser, stream)) {
            throw new ParserException("Validation was turned on, but the xml file is not valid against the schema.");
        }

        try {
            final SAXReader reader = new SAXReader();
            this.profile = reader.read(stream);
        } catch (final DocumentException e) {
            log.error("An error occurred while reading the profile: {}", e.getMessage());
            this.profile = null;
        }

        try {
            stream.close();
        } catch (final IOException e) {
            log.error("An error occurred while closing the input stream: {}", e.getMessage());
        }
    }

    /**
     * Gets the collection identifier.
     * 
     * @return the id.
     */
    public String getCollectionId() {
        return this.profile.getRootElement().attributeValue("collection");
    }

    /**
     * Gets the partition filter key (used by c3po). Note that only the first
     * partition is used.
     * 
     * @return the partition filter key.
     */
    public String getPartitionFilterKey() {
        return this.profile.getRootElement().element("partition").element("filter").attributeValue("key");
    }

    /**
     * Gets the objects count in the partition.
     * 
     * @return the count of objects.
     */
    public String getObjectsCountInPartition() {
        return this.profile.getRootElement().element("partition").attributeValue("count");
    }

    /**
     * Gets a human readable description of the objects in the form of
     * {@link C3POProfileParser#DESCRIPTION_OF_SAMPLES_TEMPLATE}
     * 
     * @return
     */
    public String getDescriptionOfObjects() {
        Element samples = (Element) this.profile.getRootElement().selectSingleNode("//partition/samples");
        String type = samples.attributeValue("type");

        return DESCRIPTION_OF_SAMPLES_TEMPLATE.replace("{1}", type);
    }

    /**
     * Gets a human readable text description of the most prominent formats in
     * the profile. Traverses the properties of the profile until it finds the
     * format distribution. Then it takes the first two most occurring formats
     * (if existing). It also appends the percentage of conflicted and unknown
     * formats if any.
     * 
     * @return the human readable description.
     */
    public String getTypeOfObjects() {
        int count = Integer.parseInt(this.getObjectsCountInPartition());
        List<Element> items = this.profile.getRootElement().selectNodes("//properties/property[@id='format']/*");

        if (items.isEmpty()) {
            return MISSING;
        }

        StringBuffer response = new StringBuffer();
        String type;
        double tmp;
        double percent;
        if (items.size() >= 1) {
            Element item = items.remove(0);
            type = item.attributeValue("id");
            tmp = Double.parseDouble(item.attributeValue("value"));
            percent = Math.floor((tmp / count) * 100);
            response.append(TYPE_OF_OBJECTS_BEGIN.replace("{1}", percent + "").replace("{2}", type));
        }

        if (items.size() >= 1) {// already removed first
            Element item = items.remove(0);
            type = item.attributeValue("id");
            tmp = Double.parseDouble(item.attributeValue("value"));
            percent = Math.floor((tmp / count) * 100);
            response.append(TYPE_OF_OBJECTS_SECOND.replace("{1}", percent + "").replace("{2}", type));
        }

        for (Object o : items) {
            Element e = (Element) o;
            if (e.attributeValue("id").equals("Conflicted")) {
                tmp = Double.parseDouble(e.attributeValue("value"));
                percent = Math.floor((tmp / count) * 100);
                response.append(TYPE_OF_OBJECTS_CONFLICTS.replace("{1}", percent + ""));
            } else if (e.attributeValue("id").equals("Unknown")) {
                tmp = Double.parseDouble(e.attributeValue("value"));
                percent = Math.floor((tmp / count) * 100);
                response.append(TYPE_OF_OBJECTS_UNKNOWN.replace("{1}", percent + ""));
            }
        }

        return response.toString();
    }

    /**
     * Gets a list of the sample objects and their metadata.
     * 
     * @return the list of {@link SampleObject}s
     */
    public List<SampleObject> getSampleObjects() {
        List<SampleObject> objects = new ArrayList<SampleObject>();
        Element samples = this.profile.getRootElement().element("partition").element("samples");
        for (Object s : samples.elements()) {
            Element sample = (Element) s;
            objects.add(this.parseSample(sample));
        }

        return objects;
    }

    /**
     * Gets a list of proprietary identifiers as specified in the profile.
     * Depending on the use of the profile and whether or not the profile is
     * associated with a repository these identifiers can be different. The only
     * guarantee provided by the profile is that all uids are unique (within a
     * profile).
     * 
     * @return a list of repository/file specific unique identifiers of the
     *         objects in the collection.
     */
    public List<String> getObjectIdentifiers() {
        List<String> uris = new ArrayList<String>();

        List<Element> elements = this.profile.getRootElement().selectNodes("//elements/element");

        for (Element e : elements) {
            uris.add(e.attributeValue("uid"));
        }

        return uris;
    }

    private SampleObject parseSample(Element sample) {
        SampleObject object = new SampleObject();

        String uid = sample.attributeValue("uid");
        String shortName = uid.substring(uid.lastIndexOf('/') + 1);

        object.setFullname(uid);
        object.setShortName(shortName);

        List nodes = sample.selectNodes("//samples/sample[@uid='" + uid + "']/record[@name='mimetype']");
        if (nodes.size() > 1) {
            object.setContentType("Conflict");
        } else if (nodes.size() == 1) {
            Element mimetype = (Element) nodes.get(0);
            object.setContentType(mimetype.attributeValue("value"));
        }

        nodes = sample.selectNodes("//samples/sample[@uid='" + uid + "']/record[@name='size']");
        if (nodes.size() == 1) {
            Element size = (Element) nodes.get(0);
            DecimalFormat format = new DecimalFormat("#.##");
            double sizeInMB = Double
                .parseDouble(format.format((Double.parseDouble(size.attributeValue("value")) / 1024 / 1024)));
            object.setSizeInMB(sizeInMB);
        }

        FormatInfo info = this.getFormatInfo(sample, object.getContentType());
        object.setFormatInfo(info);

        return object;
    }

    private FormatInfo getFormatInfo(Element sample, String mime) {
        FormatInfo info = new FormatInfo();
        info.setMimeType(mime);

        String uid = sample.attributeValue("uid");

        List nodes = sample.selectNodes("//samples/sample[@uid='" + uid + "']/record[@name='format']");
        if (nodes.size() > 1) {
            info.setName("Conflict");
        } else if (nodes.size() == 1) {
            Element format = (Element) nodes.get(0);
            info.setName(format.attributeValue("value"));
        }

        nodes = sample.selectNodes("//samples/sample[@uid='" + uid + "']/record[@name='format_version']");
        if (nodes.size() > 1) {
            info.setVersion("Conflict");
        } else if (nodes.size() == 1) {
            Element version = (Element) nodes.get(0);
            info.setVersion(version.attributeValue("value"));
        }

        nodes = sample.selectNodes("//samples/sample[@uid='" + uid + "']/record[@name='puid']");
        if (nodes.size() > 1) {
            info.setPuid("Conflict");
        } else if (nodes.size() == 1) {
            Element puid = (Element) nodes.get(0);
            info.setPuid(puid.attributeValue("value"));
        }

        return info;
    }

    // TODO read in the schema and if it is not of c3po - validate agains
    // the current schema of c3po.
    private boolean isValid(SAXParser parser, InputStream stream) {
        log.debug("validating collection profile");
        try {
            SimpleErrorHandler errorHandler = new SimpleErrorHandler();

            SAXReader reader = new SAXReader(parser.getXMLReader());
            reader.setValidation(true);

            reader.setErrorHandler(errorHandler);
            reader.read(stream);

            return errorHandler.isValid();

        } catch (SAXException e) {
            log.error("SAXException: {}", e.getMessage());
        } catch (DocumentException e) {
            e.printStackTrace();
            log.error("DocumentException: {}", e.getMessage());
        } catch (NullPointerException e) {
            log.warn("Factory is not initialized. Did you call init()");
        }

        return false;
    }

    /**
     * A simple error handler to catch if the xml document has some errors.
     * 
     * @author Petar Petrov - <me@petarpetrov.org>
     * 
     */
    private class SimpleErrorHandler implements ErrorHandler {
        private boolean valid;

        public SimpleErrorHandler() {
            this.valid = true;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            log.error("Error: {}", e.getMessage());
            this.valid = false;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            log.error("Fatal Error: {}", e.getMessage());
            this.valid = false;

        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            log.error("Warning: {}", e.getMessage());

        }

        public boolean isValid() {
            return this.valid;
        }

    }

}
