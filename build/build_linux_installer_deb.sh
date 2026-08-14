version=$(cat version.txt)
year=$(cat year.txt)
rm -rf output_linux_installer_deb
jpackage --type deb \
	--app-version $version \
	--copyright "Brandon Li ($year)" \
	--description "Brandon's Semiconductor Simulator 3D" \
	--name SemiSim \
	--icon ../images/icon.png \
	--vendor "Brandon Li" \
	--input ../target/ \
	--dest output_linux_installer_deb \
	--main-class electrodynamics.SemiSim \
	--main-jar SemiSim-$version.jar \
	--java-options "-XX:-TieredCompilation -XX:CompileThresholdScaling=0.25 --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED" \
	--about-url "https://brandonli.net/semisim" \
	--file-associations semisim.properties \
	--license-file ../license.txt \
	--linux-shortcut