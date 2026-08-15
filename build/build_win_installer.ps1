$version = Get-Content ./version.txt -Raw
$year = Get-Content ./year.txt -Raw
del output_win_installer -Recurse -Force
jpackage --type exe `
	--app-version $version `
	--copyright "Brandon Li ($year)" `
	--description "Brandon's Semiconductor Simulator 3D" `
	--name SemiSim3D `
	--icon ..\\images\\icon.ico `
	--vendor "Brandon Li" `
	--input ..\\target\\ `
	--dest output_win_installer\\ `
	--main-class electrodynamics.SemiSim `
	--main-jar SemiSim3D-$version.jar `
	--java-options "-XX:-TieredCompilation -XX:CompileThresholdScaling=0.25 --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED" `
	--about-url "https://brandonli.net/semisim" `
	--file-associations semisim.properties `
	--license-file ..\\license.txt `
	--win-dir-chooser `
	--win-menu `
	--win-menu-group "SemiSim" `
	--win-shortcut `
	--win-shortcut-prompt