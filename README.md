# dpaviser-qa-tool
Initial Quality Assurance tool for Digital Legal Deposit of Danish newspapers.

---

Run Main with the path of an infomedia batch as the first argument

---


Command line snippet for validating multiple Infomedia XML files (adapt as needed):

    for i in e*xml ;do  echo $i; xmllint --noout $i --schema src/main/resources/NewsML_1.2-infomedia.xsd ;done

To enable validation of a NewsML-infomedia XML file add the following to the root NewsML node (valid for /bad):

    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../src/main/resources/NewsML_1.2-infomedia.xsd"

Original (but formatted) NewsML_1.2.xsd added to allow easy comparison.