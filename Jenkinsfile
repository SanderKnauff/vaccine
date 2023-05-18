#!/usr/bin/env groovy

pipeline {
    agent {
        docker { image 'maven:3.9.2-eclipse-temurin' }
    }

    stages {
        stage('clean') {
            sh 'mvn clean'
        }
        stage('build') {
            sh 'mvn compile'
        }
        stage('test') {
            sh 'mvn test'
        }
        stage('package') {
            sh 'mvn test'
        }
        post {
            always {
                junit 'target/surefire-reports/*.xml'
            }
        }
    }
}
