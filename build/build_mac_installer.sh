version=$(cat version.txt)
year=$(cat year.txt)
rm -rf output_mac_installer/
jpackage --type dmg \
	--app-version $version \
	--copyright "Brandon Li ($year)" \
	--description "Brandon's Semiconductor Simulator 3D" \
	--name SemiSim \
	--icon ../images/icon.icns \
	--vendor "Brandon Li" \
	--input ../target/ \
	--dest output_mac_installer \
	--main-class electrodynamics.SemiSim \
	--main-jar SemiSim-$version.jar \
	--java-options "-XX:-TieredCompilation -XX:CompileThresholdScaling=0.25 --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED" \
	--about-url "https://brandonli.net/semisim" \
	--file-associations semisim.properties \
	--license-file ../license.txt