FROM node:18

ARG CTK_URL=https://tmf-open-api-table-documents.s3.eu-west-1.amazonaws.com/OpenApiTable/4.0.0/ctk/TMF666-Account-v4.0.0.zip
ARG CTK_SCRIPT_FOLDER=TMF666-Account
ENV CTK_SCRIPT_FOLDER=$CTK_SCRIPT_FOLDER
ENV CTK_RUN_SCRIPT=Mac-Linux-RUNCTK.sh

RUN echo $MODULE_MAME
RUN apt-get install unzip

RUN mkdir /results
RUN mkdir /opt/ctk

WORKDIR /opt/ctk

RUN wget $CTK_URL
RUN for FILE in *; do unzip $FILE; done

WORKDIR /opt/ctk/$CTK_SCRIPT_FOLDER
RUN chmod -R +x ./

COPY runTest.sh /opt/ctk/$CTK_SCRIPT_FOLDER
RUN chmod -R +x runTest.sh
CMD ./runTest.sh