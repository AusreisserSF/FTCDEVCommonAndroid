package org.firstinspires.ftc.ftcdevcommon;

import javax.xml.xpath.*;

import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Generic access to XML elements via XPath.
public class XPathAccess {

    // --------- CLASS VARIABLES ----------
    private static final String TAG = "XPathAccess";

    private final XPath xpath;
    private final Element xmlElement;

    // --------- CONSTRUCTORS ----------
    //## Compromise: pass an XML element in to the constructor.
    // It would be possible to pass the element in to every method
    // but the syntax is simpler this way.
    public XPathAccess(RobotXMLElement pRobotXMLElement) {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();
        xmlElement = pRobotXMLElement.getRobotXMLElement();
    }

    // --------- FUNCTIONS ----------

    // Works for both elements and attributes. The element or attribute
    // must be present and its associated text must not be empty.
    public String getRequiredString(String pPath) throws XPathExpressionException {
        // Crude but effective in our environment: if the path includes a '@'
        // consider it to be an XML attribute.
        String text;
        if (pPath.contains("@"))
            text = getAttributeValue(pPath);
        else
            text = getElementText(pPath);

        if (text.isEmpty())
            throw new AutonomousRobotException(TAG, "Requested item " + pPath + " does not exist in " + xmlElement.getTagName());

        return text;
    }

    // If the requested text is not present, this method returns the default value.
    // Otherwise it returns the text of the element or attribute.
    public String getString(String pPath, String pDefaultIfMissing) throws XPathExpressionException {
        // Crude but effective in our environment: if the path includes a '@'
        // consider it to be an XML attribute.
        String text;
        if (pPath.contains("@"))
            text = getAttributeValue(pPath);
        else
            text = getElementText(pPath);

        if (text.isEmpty())
            return pDefaultIfMissing;

        return text;
    }

    // Gets a text string from an element or attribute and checks it against a list
    // of valid values.
    public String getRequiredStringInRange(String pPath, List<String> pRangeList) throws XPathExpressionException {
        String text = getRequiredString(pPath);
        if (!pRangeList.contains(text))
            throw new AutonomousRobotException(TAG, "Text value " + text + " is not a valid value");

        return text;
    }

    // Gets a text string from an element or attribute and checks it against a list
    // of valid values.
    public String getStringInRange(String pPath, String pDefaultIfMissing, List<String> pRangeList) throws XPathExpressionException {
        String text = getString(pPath, pDefaultIfMissing);
        if (text.equals(pDefaultIfMissing))
            return text;

        if (!pRangeList.contains(text))
            throw new AutonomousRobotException(TAG, "Text value " + text + " is not valid");

        return text;
    }

    public List<String> validRange(String... pRangeValues) {
        List<String> finalRangeList = new ArrayList<>();

        if (pRangeValues == null)
            return finalRangeList;

        finalRangeList.addAll(Arrays.asList(pRangeValues));
        return finalRangeList;
    }

    // Works with both attributes and elements.
    public double getRequiredDouble(String pPath) throws XPathExpressionException {
        String text = getRequiredString(pPath);
        return getDoubleFromText(text, pPath);
    }

    // Works with both attributes and elements.
    public double getDouble(String pPath, double pDefaultValue) throws XPathExpressionException {
        String defaultString = Double.toString(pDefaultValue);
        String text = getString(pPath, defaultString);
        if (text.equals(defaultString))
            return pDefaultValue;

        return getDoubleFromText(text, pPath);
    }

    // Works with both attributes and elements.
    public int getRequiredInt(String pPath) throws XPathExpressionException {
        String text = getRequiredString(pPath);
        return getIntFromText(text, pPath);
    }

    // Works with both attributes and elements.
    public int getInt(String pPath, int pDefaultValue) throws XPathExpressionException {
        String defaultString = Integer.toString(pDefaultValue);
        String text = getString(pPath, defaultString);
        if (text.equals(defaultString))
            return pDefaultValue;

        return getIntFromText(text, pPath);
    }

    // Works with both attributes and elements.
    public boolean getRequiredBoolean(String pPath) throws XPathExpressionException {
        String text = getRequiredString(pPath);
       return getBooleanFromText(text, pPath);
    }

    // Works with both attributes and elements.
    public boolean getBoolean(String pPath, boolean pDefaultValue) throws XPathExpressionException {
        String defaultString = Boolean.toString(pDefaultValue);
        String text = getString(pPath, defaultString);
        if (text.equals(defaultString))
            return pDefaultValue;

        return getBooleanFromText(text, pPath);
    }

    // Returns an empty string if the attribute does not exist or
    // the attribute value is an empty string.
    private String getAttributeValue(String pPath) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(pPath);
        // Trim needed because only validating parsers will strip white space.
        return ((String) expr.evaluate(xmlElement, XPathConstants.STRING)).trim();
    }

    // Returns an empty string if the element does not exist or
    // the element's text value is an empty string.
    private String getElementText(String pPath) throws XPathExpressionException {
        XPathExpression expr = xpath.compile(pPath + "/text()");
        // Trim needed because only validating parsers will strip white space.
        return ((String) expr.evaluate(xmlElement, XPathConstants.STRING)).trim();
    }

    private double getDoubleFromText(String pDoubleText, String pNodeName) {
        if (pDoubleText.isEmpty())
            throw new AutonomousRobotException(TAG, "Requested item " + pNodeName + " does not exist in " + xmlElement.getTagName());

        double itemDouble;
        try {
            itemDouble = Double.parseDouble(pDoubleText);
        } catch (NumberFormatException ex) {
            throw new AutonomousRobotException(TAG, "Value in " + pNodeName + ": " + pDoubleText + " is not a double");
        }

        return itemDouble;
    }

    private int getIntFromText(String pIntText, String pNodeName) {
        if (pIntText.isEmpty())
            throw new AutonomousRobotException(TAG, "Requested item " + pIntText + " does not exist in " + xmlElement.getTagName());

        int itemInt;
        try {
            itemInt = Integer.parseInt(pIntText);
        } catch (NumberFormatException ex) {
            throw new AutonomousRobotException(TAG, "Value in " + pNodeName + ": " + pIntText + " is not an int");
        }

        return itemInt;
    }

    private boolean getBooleanFromText(String pBoolText, String pNodeName) {
        if (pBoolText.isEmpty())
            throw new AutonomousRobotException(TAG, "Requested item " + pNodeName + " does not exist in " + xmlElement.getTagName());

        if (pBoolText.equals("true"))
            return true;
        if (pBoolText.equals("false"))
            return false;
        throw new AutonomousRobotException(TAG, "Value in " + pNodeName + ": " + pBoolText + " is not a boolean");
    }
}
