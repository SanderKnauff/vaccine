#!/usr/bin/env groovy

pipeline {
    agent {
        docker { image 'maven:3.9.2-eclipse-temurin' }
    }

    stages {
        stage('clean') {
            steps {
                sh 'mvn clean'
            }
        }
        stage('build') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('package') {
            steps {
                sh 'mvn test'
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml'
        }
    }
}
