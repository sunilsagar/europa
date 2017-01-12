package com.distelli.europa.handlers;

import org.eclipse.jetty.http.HttpMethod;
import com.distelli.webserver.RequestHandler;
import com.distelli.webserver.WebResponse;
import com.distelli.webserver.RequestContext;
import com.fasterxml.jackson.databind.JsonNode;
import javax.inject.Singleton;
import java.util.Map;
import java.util.HashMap;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.distelli.europa.db.RegistryManifestDb;
import com.distelli.europa.util.ObjectKeyFactory;
import com.distelli.objectStore.ObjectKey;
import com.distelli.objectStore.ObjectStore;
import com.distelli.objectStore.ObjectMetadata;
import com.distelli.europa.models.RegistryManifest;
import com.distelli.europa.registry.RegistryError;
import com.distelli.europa.registry.RegistryErrorCode;

@Log4j
@Singleton
public class RegistryManifestExists extends RegistryBase {
    @Inject
    private ObjectKeyFactory _objectKeyFactory;
    @Inject
    private ObjectStore _objectStore;
    @Inject
    private RegistryManifestDb _manifestDb;

    public WebResponse handleRegistryRequest(RequestContext requestContext) {
        String name = requestContext.getMatchedRoute().getParam("name");
        String reference = requestContext.getMatchedRoute().getParam("reference");

        RegistryManifest manifest = _manifestDb.getManifestByRepoTag(name, reference);
        if ( null == manifest ) {
            throw new RegistryError(
                "No manifest exists with name="+name+" reference="+reference,
                RegistryErrorCode.MANIFEST_UNKNOWN);
        }

        ObjectKey objKey = _objectKeyFactory.forRegistryManifest(manifest.getManifestId());

        ObjectMetadata objMeta = _objectStore.head(objKey);
        if ( null == objMeta ) {
            throw new RegistryError(
                "Manifest is missing from object store. "+objKey,
                RegistryErrorCode.MANIFEST_UNKNOWN);
        }

        WebResponse response = new WebResponse(200);
                        
        // TODO: Capture this and return it properly...
        response.setContentType("application/vnd.docker.distribution.manifest.v1+json");
        response.setResponseHeader("Content-Length", ""+objMeta.getContentLength());
        return response;
    }
}