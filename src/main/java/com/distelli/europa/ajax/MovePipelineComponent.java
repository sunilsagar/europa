package com.distelli.europa.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import com.distelli.europa.clients.*;
import com.distelli.europa.db.*;
import com.distelli.europa.models.*;
import com.distelli.gcr.*;
import com.distelli.gcr.auth.*;
import com.distelli.gcr.models.*;
import com.distelli.persistence.*;
import com.distelli.webserver.*;
import com.google.inject.Singleton;
import org.eclipse.jetty.http.HttpMethod;
import lombok.extern.log4j.Log4j;
import javax.inject.Inject;
import com.distelli.europa.EuropaRequestContext;

@Log4j
@Singleton
public class MovePipelineComponent extends AjaxHelper<EuropaRequestContext>
{
    @Inject
    private PipelineDb _db;

    public MovePipelineComponent()
    {
        this.supportedHttpMethods.add(HTTPMethod.POST);
    }

    public Object get(AjaxRequest ajaxRequest, EuropaRequestContext requestContext)
    {
        // TODO - implement
        String domain = requestContext.getOwnerDomain();
        return null;
    }
}
