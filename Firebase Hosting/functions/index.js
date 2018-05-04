const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
exports.helloWorld = functions.https.onRequest((request, response) => {
    response.send("Hello from Firebase!");
});

exports.templateChange = functions.database.ref('/CourseTemplates/{id}').onWrite(event => {
    const template = (event.data.val()) ? event.data.val() : event.data.previous.val();
    const pTemplate = event.data.previous.val();
    const owner = template.ownerId;
    const iName = template.edKey.replace(/%&[\s\S]*/, '');
    const dName = template.edKey.replace(/[\s\S]*%&/, '');
    const inst = iName.replace(/\s+/g, '_');
    const dept = dName.replace(/\s+/g, '_');

    if (event.data.previous.val() && event.data.current.val()) {
        return console.log('This is an update');
    }

    return admin.database().ref(`/Users/${owner}/tempCount`).transaction(count => {
        if (!pTemplate) {
            if (count) count++;
            else count = 1;
        } else {
            if (count) {
                if (count < 1) count = 0;
                else count--;
            } else count = 0;
        }
        return count;
    }).then(() => {
        return admin.database().ref(`/Eds/${inst}/${dept}`).transaction(data => {

            if (!data) data = { name: dName, count: 0 }
            if (!pTemplate) {
                if (data.count) data.count++;
                else data.count = 1;
            } else {
                if (data.count) {
                    if (data.count < 1) data.count = 0;
                    else data.count--;
                } else data.count = 0;
            }
            return data;
        }).then(() => {

            return admin.database().ref(`/Eds/${inst}/${dept}/count`).once('value').then(snapshot => {

                const fCount = snapshot.val();

                return admin.database().ref(`/Institutions/${inst}`).transaction(data => {
                    if (!data) data = { name: iName, count: 0 }
                    if (!pTemplate) {
                        if (fCount == 1) {
                            if (data.count) data.count++;
                            else data.count = 1;
                        }
                    } else {
                        if (fCount == 0) {
                            if (data.count) {
                                if (data.count < 1) data.count = 0;
                                else data.count--;
                            } else data.count = 0;
                        }
                    }
                    return data;
                }).then(() => console.log("Template updated"));
            });

        });
    });

});


exports.userNameChange = functions.database.ref('/Users/{id}/userName').onWrite(event => {
    const userId = event.params.id;
    const userName = event.data.val();

    if (!event.data.previous.val()) {
        return console.log('new userName');
    }

    return admin.database().ref('CourseTemplates').orderByChild('ownerId').equalTo(userId).once('value').then(snapshot => {

        const promises = [];

        snapshot.forEach(child => {
            promises.push(admin.database().ref('CourseTemplates').child(child.key).child('ownerName').set(userName));
        });

        return Promise.all(promises).then(() => console.log('userName updated successfully'));
    });

});

exports.edKeyChanged = functions.database.ref('/Users/{id}').onWrite(event => {
    const user = event.data.val();
    const userId = event.params.id;
    const pUser = event.data.previous.val();

    if (!pUser) {
        return console.log('new user');
    }

    if (pUser.institution == user.institution && pUser.department == user.department) {
        return console.log('edKey not changed');
    }

    return admin.database().ref('CourseTemplates').orderByChild('ownerId').equalTo(userId).once('value').then(snapshot => {

        const promises = [];

        snapshot.forEach(child => {
            const edKey = user.institution + '%&' + user.department;
            promises.push(admin.database().ref('CourseTemplates').child(child.key).child('edKey').set(edKey));
        });

        return Promise.all(promises).then(() => {

            const pInst = pUser.institution.replace(/\s+/g, '_');
            const pDept = pUser.department.replace(/\s+/g, '_');

            return admin.database().ref(`/Eds/${pInst}/${pDept}`).transaction(data => {

                if (!data) data = { name: pUser.department, count: 0 }

                if (data.count) {
                    if (data.count < 1) data.count = 0;
                    else data.count -= pUser.tempCount;
                } else data.count = 0;

                return data;
            }).then(() => {

                return admin.database().ref(`/Eds/${pInst}/${pDept}/count`).once('value').then(snapshot => {
                    const icount = snapshot.val();

                    return admin.database().ref(`/Institutions/${pInst}`).transaction(data => {

                        if (!data) data = { name: pUser.institution, count: 0 }

                        if (icount == 0) {
                            if (data.count) {
                                if (data.count < 1) data.count = 0;
                                else data.count--;
                            } else data.count = 0;
                        }

                        return data;
                    }).then(() => {

                        const nInst = user.institution.replace(/\s+/g, '_');
                        const nDept = user.department.replace(/\s+/g, '_');

                        return admin.database().ref(`/Eds/${nInst}/${nDept}`).transaction(data => {

                            if (!data) data = { name: user.department, count: 0 }

                            if (data.count) data.count += user.tempCount;
                            else data.count = user.tempCount;

                            return data;
                        }).then(() => {

                            return admin.database().ref(`/Eds/${nInst}/${nDept}/count`).once('value').then(snapshot => {
                                const fCount = snapshot.val();

                                return admin.database().ref(`/Institutions/${nInst}`).transaction(data => {
                                    if (!data) data = { name: user.institution, count: 0 }

                                    if (fCount == user.tempCount) {
                                        if (data.count) data.count++;
                                        else data.count = 1;
                                    }

                                    return data;
                                }).then(() => console.log('Updated template'));
                            });

                        });
                    });
                });

            });
        });
    });
});

exports.onRequest = functions.database.ref('/Requests/{id}').onWrite(event => {

});