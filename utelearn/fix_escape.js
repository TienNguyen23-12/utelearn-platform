const fs = require('fs');
const path = require('path');
const baseDir = 'd:/Izhary/HK1_2627/LTWeb/utelearn-platform/utelearn/src/main/resources/templates';

function getFiles(dir, fileList = []) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const filePath = path.join(dir, file);
        if (fs.statSync(filePath).isDirectory()) {
            getFiles(filePath, fileList);
        } else if (filePath.endsWith('.html')) {
            fileList.push(filePath);
        }
    }
    return fileList;
}

const htmlFiles = getFiles(baseDir);

for (const file of htmlFiles) {
    let content = fs.readFileSync(file, 'utf8');
    if (content.includes('\\`')) {
        content = content.replace(/\\`/g, '`');
        fs.writeFileSync(file, content, 'utf8');
        console.log('Fixed: ' + file);
    }
}
