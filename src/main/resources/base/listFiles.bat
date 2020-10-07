
find ./src -type f -print | sed -e "s/$/<br>/g" >> README.md

pause