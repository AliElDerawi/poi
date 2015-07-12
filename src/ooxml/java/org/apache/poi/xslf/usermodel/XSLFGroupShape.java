/*
 *  ====================================================================
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * ====================================================================
 */

package org.apache.poi.xslf.usermodel;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.opc.*;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.util.*;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.*;
import org.openxmlformats.schemas.presentationml.x2006.main.*;

/**
 * Represents a group shape that consists of many shapes grouped together.
 * 
 * @author Yegor Kozlov
 */
@Beta
public class XSLFGroupShape extends XSLFShape implements XSLFShapeContainer, GroupShape<XSLFShape> {
    private static POILogger _logger = POILogFactory.getLogger(XSLFGroupShape.class);
    
    private final List<XSLFShape> _shapes;
    private final CTGroupShapeProperties _grpSpPr;
    private XSLFDrawing _drawing;

    protected XSLFGroupShape(CTGroupShape shape, XSLFSheet sheet){
        super(shape,sheet);
        _shapes = sheet.buildShapes(shape);
        _grpSpPr = shape.getGrpSpPr();
    }

    protected CTGroupShapeProperties getGrpSpPr() {
        return _grpSpPr;
    }
    
    protected CTGroupTransform2D getSafeXfrm() {
        CTGroupTransform2D xfrm = getXfrm();
        return (xfrm == null ? getGrpSpPr().addNewXfrm() : xfrm);
    }
    
    protected CTGroupTransform2D getXfrm() {
        return getGrpSpPr().getXfrm();
    }

    @Override
    public Rectangle2D getAnchor(){
        CTGroupTransform2D xfrm = getXfrm();
        CTPoint2D off = xfrm.getOff();
        long x = off.getX();
        long y = off.getY();
        CTPositiveSize2D ext = xfrm.getExt();
        long cx = ext.getCx();
        long cy = ext.getCy();
        return new Rectangle2D.Double(
                Units.toPoints(x), Units.toPoints(y),
                Units.toPoints(cx), Units.toPoints(cy));
    }

    @Override
    public void setAnchor(Rectangle2D anchor){
        CTGroupTransform2D xfrm = getSafeXfrm();
        CTPoint2D off = xfrm.isSetOff() ? xfrm.getOff() : xfrm.addNewOff();
        long x = Units.toEMU(anchor.getX());
        long y = Units.toEMU(anchor.getY());
        off.setX(x);
        off.setY(y);
        CTPositiveSize2D ext = xfrm.isSetExt() ? xfrm.getExt() : xfrm.addNewExt();
        long cx = Units.toEMU(anchor.getWidth());
        long cy = Units.toEMU(anchor.getHeight());
        ext.setCx(cx);
        ext.setCy(cy);
    }

    /**
     *
     * @return the coordinates of the child extents rectangle
     * used for calculations of grouping, scaling, and rotation
     * behavior of shapes placed within a group.
     */
    public Rectangle2D getInteriorAnchor(){
        CTGroupTransform2D xfrm = getXfrm();
        CTPoint2D off = xfrm.getChOff();
        long x = off.getX();
        long y = off.getY();
        CTPositiveSize2D ext = xfrm.getChExt();
        long cx = ext.getCx();
        long cy = ext.getCy();
        return new Rectangle2D.Double(
                Units.toPoints(x), Units.toPoints(y),
                Units.toPoints(cx), Units.toPoints(cy));
    }

    /**
     *
     * @param anchor the coordinates of the child extents rectangle
     * used for calculations of grouping, scaling, and rotation
     * behavior of shapes placed within a group.
     */
    public void setInteriorAnchor(Rectangle2D anchor) {
        CTGroupTransform2D xfrm = getSafeXfrm();
        CTPoint2D off = xfrm.isSetChOff() ? xfrm.getChOff() : xfrm.addNewChOff();
        long x = Units.toEMU(anchor.getX());
        long y = Units.toEMU(anchor.getY());
        off.setX(x);
        off.setY(y);
        CTPositiveSize2D ext = xfrm.isSetChExt() ? xfrm.getChExt() : xfrm.addNewChExt();
        long cx = Units.toEMU(anchor.getWidth());
        long cy = Units.toEMU(anchor.getHeight());
        ext.setCx(cx);
        ext.setCy(cy);
    }

    /**
     *
     * @return child shapes contained witin this group
     */
    @Override
    public List<XSLFShape> getShapes(){
        return _shapes;
    }

    /**
     * Returns an iterator over the shapes in this sheet
     *
     * @return an iterator over the shapes in this sheet
     */
    public Iterator<XSLFShape> iterator(){
        return _shapes.iterator();
    }

    /**
     * Remove the specified shape from this group
     */
    public boolean removeShape(XSLFShape xShape) {
        XmlObject obj = xShape.getXmlObject();
        CTGroupShape grpSp = (CTGroupShape)getXmlObject();
        if(obj instanceof CTShape){
            grpSp.getSpList().remove(obj);
        } else if (obj instanceof CTGroupShape){
            grpSp.getGrpSpList().remove(obj);
        } else if (obj instanceof CTConnector){
            grpSp.getCxnSpList().remove(obj);
        } else {
            throw new IllegalArgumentException("Unsupported shape: " + xShape);
        }
        return _shapes.remove(xShape);
    }

    /**
     * @param shapeId 1-based shapeId
     */
    static CTGroupShape prototype(int shapeId) {
        CTGroupShape ct = CTGroupShape.Factory.newInstance();
        CTGroupShapeNonVisual nvSpPr = ct.addNewNvGrpSpPr();
        CTNonVisualDrawingProps cnv = nvSpPr.addNewCNvPr();
        cnv.setName("Group " + shapeId);
        cnv.setId(shapeId + 1);

        nvSpPr.addNewCNvGrpSpPr();
        nvSpPr.addNewNvPr();
        ct.addNewGrpSpPr();
        return ct;
    }

    // shape factory methods
    private XSLFDrawing getDrawing(){
        if(_drawing == null) {
            _drawing = new XSLFDrawing(getSheet(), (CTGroupShape)getXmlObject());
        }
        return _drawing;
    }

    public XSLFAutoShape createAutoShape(){
        XSLFAutoShape sh = getDrawing().createAutoShape();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFFreeformShape createFreeform(){
        XSLFFreeformShape sh = getDrawing().createFreeform();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFTextBox createTextBox(){
        XSLFTextBox sh = getDrawing().createTextBox();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFConnectorShape createConnector(){
        XSLFConnectorShape sh = getDrawing().createConnector();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFGroupShape createGroup(){
        XSLFGroupShape sh = getDrawing().createGroup();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFPictureShape createPicture(int pictureIndex){

        List<PackagePart>  pics = getSheet().getPackagePart().getPackage()
                .getPartsByName(Pattern.compile("/ppt/media/image" + (pictureIndex + 1) + ".*?"));

        if(pics.size() == 0) {
            throw new IllegalArgumentException("Picture with index=" + pictureIndex + " was not found");
        }

        PackagePart pic = pics.get(0);

        PackageRelationship rel = getSheet().getPackagePart().addRelationship(
                pic.getPartName(), TargetMode.INTERNAL, XSLFRelation.IMAGES.getRelation());

        XSLFPictureShape sh = getDrawing().createPicture(rel.getId());
        sh.resize();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }

    public XSLFTable createTable(){
        XSLFTable sh = getDrawing().createTable();
        _shapes.add(sh);
        sh.setParent(this);
        return sh;
    }
    
    @Override
    public void setFlipHorizontal(boolean flip){
        getSafeXfrm().setFlipH(flip);
    }

    @Override
    public void setFlipVertical(boolean flip){
        getSafeXfrm().setFlipV(flip);
    }

    @Override
    public boolean getFlipHorizontal(){
        CTGroupTransform2D xfrm = getXfrm();
        return (xfrm == null || !xfrm.isSetFlipH()) ? false : xfrm.getFlipH();
    }

    @Override
    public boolean getFlipVertical(){
        CTGroupTransform2D xfrm = getXfrm();
        return (xfrm == null || !xfrm.isSetFlipV()) ? false : xfrm.getFlipV();
    }

    @Override
    public void setRotation(double theta){
        getSafeXfrm().setRot((int) (theta * 60000));
    }

    @Override
    public double getRotation(){
        CTGroupTransform2D xfrm = getXfrm();
        return (xfrm == null || !xfrm.isSetRot()) ? 0 : (xfrm.getRot() / 60000.d);
    }

    @Override
    void copy(XSLFShape src){
        XSLFGroupShape gr = (XSLFGroupShape)src;
        
        // clear shapes
        clear();
        
        // recursively update each shape
        for(XSLFShape shape : gr.getShapes()) {
            XSLFShape newShape = null;
            if (shape instanceof XSLFTextBox) {
                newShape = createTextBox();
            } else if (shape instanceof XSLFAutoShape) {
                newShape = createAutoShape();
            } else if (shape instanceof XSLFConnectorShape) {
                newShape = createConnector();
            } else if (shape instanceof XSLFFreeformShape) {
                newShape = createFreeform();
            } else if (shape instanceof XSLFPictureShape) {
                XSLFPictureShape p = (XSLFPictureShape)shape;
                XSLFPictureData pd = p.getPictureData();
                int picId = getSheet().getSlideShow().addPicture(pd.getData(), pd.getPictureType());
                newShape = createPicture(picId);
            } else if (shape instanceof XSLFGroupShape) {
                newShape = createGroup();
            } else if (shape instanceof XSLFTable) {
                newShape = createTable();
            } else {
                _logger.log(POILogger.WARN, "copying of class "+shape.getClass()+" not supported.");
                continue;
            }

            newShape.copy(shape);
        }
    }

    /**
     * Removes all of the elements from this container (optional operation).
     * The container will be empty after this call returns.
     */
    public void clear() {
        List<XSLFShape> shapes = new ArrayList<XSLFShape>(getShapes());
        for(XSLFShape shape : shapes){
            removeShape(shape);
        }
    }

    public void addShape(XSLFShape shape) {
        throw new UnsupportedOperationException(
            "Adding a shape from a different container is not supported -"
            + " create it from scratch with XSLFGroupShape.create* methods");
    }
}