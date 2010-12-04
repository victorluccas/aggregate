/*
 * Copyright (C) 2009 Google Inc. 
 * Copyright (C) 2010 University of Washington.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.constants.ServletConsts;
import org.opendatakit.aggregate.datamodel.FormElementKey;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.form.Form;
import org.opendatakit.aggregate.format.SubmissionFormatter;
import org.opendatakit.aggregate.format.structure.KmlFormatter;
import org.opendatakit.aggregate.query.submission.QueryByDate;
import org.opendatakit.common.constants.BasicConsts;
import org.opendatakit.common.constants.HtmlConsts;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.UserService;

/**
 * Servlet to generate a KML file for download
 * 
 * @author alerer@gmail.com
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class KmlServlet extends ServletUtilBase {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = 2387155275645640699L;

  /**
   * URI from base
   */
  public static final String ADDR = "kml";

  public static final String IMAGE_FIELD = "imageField";

  public static final String TITLE_FIELD = "titleField";

  public static final String GEOPOINT_FIELD = "geopointField";

  /**
   * Handler for HTTP Get request that responds with CSV
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    // verify user is logged in
    if (!verifyCredentials(req, resp)) {
      return;
    }

    UserService userService = (UserService) ContextFactory.get().getBean(BeanDefs.USER_BEAN);
    User user = userService.getCurrentUser();

    // get parameter
    String formId = getParameter(req, ServletConsts.FORM_ID);

    String geopointFieldName = getParameter(req, GEOPOINT_FIELD);
    String titleFieldName = getParameter(req, TITLE_FIELD);
    String imageFieldName = getParameter(req, IMAGE_FIELD);

    if (formId == null || geopointFieldName == null) {
      errorMissingKeyParam(resp);
      return;
    }

    Datastore ds = (Datastore) ContextFactory.get().getBean(BeanDefs.DATASTORE_BEAN);

    Form form = null;
    try {
      form = Form.retrieveForm(formId, ds, user);
     
      FormElementModel titleField = null;
      if (titleFieldName != null) {
        FormElementKey titleKey = new FormElementKey(titleFieldName);
        titleField = FormElementModel.retrieveFormElementModel(form, titleKey);
      }
      FormElementModel geopointField = null;
      if (geopointFieldName != null) {
        FormElementKey geopointKey = new FormElementKey(geopointFieldName);
        geopointField = FormElementModel.retrieveFormElementModel(form, geopointKey);
      }
      FormElementModel imageField = null;
      if (imageFieldName != null) {
        if(!imageFieldName.equals(KmlSettingsServlet.NONE)) {
          FormElementKey imageKey = new FormElementKey(imageFieldName);
          imageField = FormElementModel.retrieveFormElementModel(form, imageKey);
        }
      }

      resp.setContentType(HtmlConsts.RESP_TYPE_XML);
      resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
      setDownloadFileName(resp, formId + ServletConsts.KML_FILENAME_APPEND);

      // create KML
      QueryByDate query = new QueryByDate(form, BasicConsts.EPOCH, false,
          ServletConsts.FETCH_LIMIT, ds, user);
      SubmissionFormatter formatter = new KmlFormatter(form, getServerURL(req), geopointField,
          titleField, imageField, resp.getWriter(), null, ds);
      formatter.processSubmissions(query.getResultSubmissions());
    } catch (ODKFormNotFoundException e) {
      odkIdNotFoundError(resp);
    } catch (ODKIncompleteSubmissionData e) {
      errorRetreivingData(resp);
    } catch (ODKDatastoreException e) {
      errorRetreivingData(resp);
    }
  }

}