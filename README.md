# dpaviser-qa-tool
Initial Quality Assurance tool for Digital Legal Deposit of Danish newspapers.

Command line snippet for validating multiple Infomedia XML files (adapt as needed):

    for i in e*xml ;do  echo $i; xmllint --noout $i --schema src/main/resources/NewsML_1.2-infomedia.xsd ;done

