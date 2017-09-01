package org.dtls.fairifier;

import java.lang.Exception;
import java.lang.System;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.deri.grefine.rdf.utils.HttpUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONWriter;
import org.json.JSONException;
import com.google.refine.commands.Command;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.turtle.TurtleParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.io.InputStream;
import nl.dtl.fairmetadata4j.io.*;
import nl.dtl.fairmetadata4j.model.*;
import java.util.List;
import nl.dtl.fairmetadata4j.utils.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.IRI;
/**
 * A command to get all the metadata from a FAIR Data Point(FDP)
 * @author Shamanou van Leeuwen
 * @date 1-11-2016
 *
 */

public class GetFairDataPointInfoCommand extends Command{
    private SimpleValueFactory f;
    
    /**
     * 
     * This method retrieves the catalog and dataset information
     * depending on what is specified in the POST call.
     * 
     * I returns the content with a "content" key.
     * 
     * When the method reports an error it returns this to the frontend.
     * 
     * @param req a request object
     * @param res a response object 
     * @see com.google.refine.commands.Command#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        f = SimpleValueFactory.getInstance();
        String uri = req.getParameter("uri");
        try{
            res.setCharacterEncoding("UTF-8");
            res.setHeader("Content-Type", "application/json");
            JSONWriter writer = new JSONWriter(res.getWriter());
            writer.object();
            if (req.getParameter("layer").equals("catalog")){
                writer.key("content"); 
                writer.value(this.getFdpCatalogs(uri));
            } else if(req.getParameter("layer").equals("dataset")){
                writer.key("content"); 
                writer.value(this.getFdpDatasets(uri));
            }
            writer.key("code"); writer.value("ok");
            writer.endObject();
        }catch(Exception e){
               try {
                res.setCharacterEncoding("UTF-8");
                res.setHeader("Content-Type", "application/json");
                JSONWriter writer = new JSONWriter(res.getWriter());
                writer.object();
                writer.key("code"); writer.value("error");
                writer.key("error"); writer.value(e.getMessage());
                writer.endObject();
               }catch (Exception ex) {
                   System.out.println(ex.getMessage());
               }
        }
    }

    /**
     * 
     * Get all datasets in a FDP based on the url of the FDP.
     * 
     * This method uses the FAIRMetadata4j library.
     * 
     * @param url
     * @return ArrayList<DatasetMetadata>
     * @throws IOException
     * @throws RDFParseException
     * @throws RDFHandlerException
     * @throws LayerUnavailableException
     */
    private ArrayList<DatasetMetadata> getFdpDatasets(String url) throws IOException, RDFParseException, RDFHandlerException, LayerUnavailableException{
        f = SimpleValueFactory.getInstance();
        ArrayList<DatasetMetadata> out = new ArrayList<DatasetMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url,"text/turtle").getContent()));
            parser.parse(reader, url);
            CatalogMetadataParser catalogMetadataParser = MetadataParserUtils.getCatalogParser();
            DatasetMetadataParser datasetMetadataParser = MetadataParserUtils.getDatasetParser(); 
            List<IRI> datasetUris = catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), f.createIRI(url)).getDatasets();
            for (IRI u : datasetUris){
                try{
                    reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString(),"text/turtle").getContent()));        
                    parser.parse(reader, u.toString());
                    out.add(datasetMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()), u));
                }catch(Exception e){
                    System.out.println("datasets could not be retrieved");
                }
            }
        }catch(Exception e){
            System.out.println("catalog could not be retrieved");
        }
        return out;
    }
    
    /**
     * 
     * Get all catalogs in a FDP based on the url of the FDP.
     * 
     * This method uses the FAIRMetadata4j library.
     * 
     * 
     * @param url
     * @return ArrayList<CatalogMetadata>
     * @throws IOException
     * @throws LayerUnavailableException
     * @throws RDFParseException
     * @throws RDFHandlerException
     */
    private ArrayList<CatalogMetadata> getFdpCatalogs(String url) throws IOException, LayerUnavailableException, RDFParseException, RDFHandlerException{
        f = SimpleValueFactory.getInstance();
        ArrayList<CatalogMetadata> out = new ArrayList<CatalogMetadata>();
        TurtleParser parser = new TurtleParser();
        StatementCollector rdfStatementCollector = new StatementCollector();
        parser.setRDFHandler(rdfStatementCollector);
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(HttpUtils.get(url,"text/turtle").getContent()));
            try{
                parser.parse(reader, url);
                FDPMetadataParser fdpParser = MetadataParserUtils.getFdpParser();
                CatalogMetadataParser catalogMetadataParser = MetadataParserUtils.getCatalogParser();
                List<IRI> catalogUris = fdpParser.parse(new ArrayList(rdfStatementCollector.getStatements()), f.createIRI(url)).getCatalogs();
                for (IRI u : catalogUris){
                    try{
                        reader = new BufferedReader(new InputStreamReader(HttpUtils.get(u.toString(),"text/turtle").getContent()));        
                        parser.parse(reader, u.toString());
                        out.add(catalogMetadataParser.parse(new ArrayList(rdfStatementCollector.getStatements()),u));
                    }catch(Exception e){
                        throw new LayerUnavailableException("catalogs could not be retrieved");
                    }
                }
            }catch(Exception ex){}
        }catch(Exception e){
            System.out.println(e.getMessage());
            throw new LayerUnavailableException("fdp could not be retrieved");
        }
        return out;
    }
}
