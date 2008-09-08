/* This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 */
package org.knime.core.node.port.pmml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.knime.core.node.port.PortObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class PMMLPortObject implements PortObject {
    
    /** Constant for CDATA. */
    protected static final String CDATA = "CDATA";
    /** Constant for DataDictionary. */
    protected static final String DATA_DICT = "DataDictionary";
    /** Constant for DataField. */
    protected static final String DATA_FIELD = "DataField";
    /** Constant for Value. */
    protected static final String VALUE = "Value";
    
    private static final String PMML_3_0 = "/schemata/pmml-3-0.xsd";

    private static final String PMML_3_1 = "/schemata/pmml-3-1.xsd";

    private static final String PMML_3_2 = "/schemata/pmml-3-2.xsd";
    
    private static final Map<String, String> version_schema_map 
        = new HashMap<String, String>();

    public static String getLocalSchemaLocation(final String version) {
        return version_schema_map.get(version);
    }
    
    static {
        version_schema_map.put("3.0", PMML_3_0);
        version_schema_map.put("3.1", PMML_3_1);
        version_schema_map.put("3.2", PMML_3_2);
    }
    
    private TransformerHandler m_handler;
    
    private PMMLPortObjectSpec m_spec;
    private PMMLModelType m_modelType;
    
    private PMMLMasterContentHandler m_masterHandler;
    
    private static PMMLPortObjectSerializer serializer;
    
    /**
     * Static serializer as demanded from {@link PortObject} framework.
     * @return serializer for PMML (reads and writes PMML files)
     */
    public static final PortObjectSerializer<PMMLPortObject> 
    getPortObjectSerializer() {
        if (serializer == null) {
            serializer = new PMMLPortObjectSerializer();
        }
        return serializer;
    }
    
    
    /**
     * Default constructor necessary for loading. Derived classes also 
     * <em>must</em> provide a default constructor, otherwise loading will fail.
     */
    public PMMLPortObject() {
        m_masterHandler = new PMMLMasterContentHandler();
    }
    
    /**
     * @param spec the referring {@link PMMLPortObjectSpec}
     */
    public PMMLPortObject(final PMMLPortObjectSpec spec) {
        m_spec = spec;
        m_masterHandler = new PMMLMasterContentHandler();
    }
    
    /**
     * Adds a content handler to the master content handler. The master content 
     * handler forwards all relevant events from PMML file parsing to all 
     * registered content handlers.
     *  
     * @param id to later on retrieve the registered content handler
     * @param defaultHandler specialized content handler interested in certain 
     * parts of the PMML file (ClusteringModel, TreeModel, etc.)
     * @return true if the handler was added, false if it is already registered
     */
    public boolean addPMMLContentHandler(final String id, 
            final PMMLContentHandler defaultHandler) {
        return m_masterHandler.addContentHandler(id, defaultHandler);
    }
    
    /**
     * 
     * @param id the id which was used for registration of the handler
     * @return the handler registered with this id or null if no handler with 
     *  this id can be found
     */
    public PMMLContentHandler getPMMLContentHandler(final String id) {
        return m_masterHandler.getDefaultHandler(id);
    }
    
    /**
     * 
     * @return the type of model
     * @see PMMLModelType
     */
    public PMMLModelType getModelType() {
        return m_modelType;
    }
    
    private void init(final OutputStream out) 
        throws TransformerConfigurationException, SAXException {
        SAXTransformerFactory fac = (SAXTransformerFactory)TransformerFactory
        .newInstance();
        m_handler = fac.newTransformerHandler();
        
        Transformer t = m_handler.getTransformer();
        t.setOutputProperty(OutputKeys.METHOD, "xml");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        
        m_handler.setResult(new StreamResult(out));
        
        // PMML root element, namespace declaration, etc.
        m_handler.startDocument();
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute(null, null, "version", CDATA, "3.1");
        attr.addAttribute(null, null, "xmlns", CDATA, 
            "http://www.dmg.org/PMML-3_1");
        attr.addAttribute(null, null, "xmlns:xsi", CDATA, 
            "http://www.w3.org/2001/XMLSchema-instance");
        m_handler.startElement(null, null, "PMML", attr);
    }
    
    
    
    /**
     * Writes the port object to valid PMML. Subclasses should not override this
     * method but the {@link #writePMMLModel(TransformerHandler)} instead.
     *    
     * 
     * @param out zipped stream which reads the PMML file 
     * @throws SAXException if something goes wrong during writing of PMML
     * @throws IOException if the file cannot be written to the directory
     * @throws TransformerConfigurationException if something goes wrong with 
     *  the transformation handler 
     */
    public void save(final OutputStream out) 
        throws SAXException, IOException, TransformerConfigurationException {
        init(out);
        PMMLPortObjectSpec.writeHeader(m_handler);
        PMMLPortObjectSpec.writeDataDictionary(getSpec().getDataTableSpec(),
                m_handler);
        writePMMLModel(m_handler);
        m_handler.endElement(null, null, "PMML");
        m_handler.endDocument();
        out.close();
    }
    
    
    
    /** {@inheritDoc} */
    @Override
    public abstract String getSummary();
    
    
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in, 
            final String version) 
            throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory fac = SAXParserFactory.newInstance();
        
        SchemaFactory schemaFac = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFac.newSchema(new StreamSource(
                getSchemaInputStream(version)));
        fac.setSchema(schema);
        fac.setNamespaceAware(true);
        
        SAXParser parser = fac.newSAXParser(); 
        // TODO: remove all X- elements!!!
        
        XFilter filter = new XFilter(version);
        filter.setParent(parser.getXMLReader());
        
        ExtractModelTypeHandler modelTypeHdl = new ExtractModelTypeHandler();
        
        m_masterHandler.addContentHandler(ExtractModelTypeHandler.ID, 
                modelTypeHdl);
        filter.setContentHandler(m_masterHandler);
        filter.parse(new InputSource(in));
        ExtractModelTypeHandler hdl = (ExtractModelTypeHandler)m_masterHandler
            .getDefaultHandler(ExtractModelTypeHandler.ID);
        m_modelType = hdl.getModelType();
        m_spec = spec;        
    }
    
    
    private InputStream getSchemaInputStream(final String version) {
        ClassLoader loader = PMMLPortObject.class.getClassLoader();
        String packagePath =
                PMMLPortObject.class.getPackage().getName().replace('.', '/');
        return loader.getResourceAsStream(
                packagePath + version_schema_map.get(version));
    }
    
    
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public PMMLPortObjectSpec getSpec() {
        return m_spec;
    }
    

    /**
     * 
     * @param handler the handler responsible for writing the PMML
     * @throws SAXException if something goes wrong during writing the PMML
     */
    protected abstract void writePMMLModel(final TransformerHandler handler)
        throws SAXException;
    
    

}
