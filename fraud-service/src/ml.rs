use smartcore::linalg::basic::matrix::DenseMatrix;
use smartcore::linear::logistic_regression::{
    LogisticRegression, LogisticRegressionParameters,
};

pub struct FraudModel {
    model: LogisticRegression<f64, i32, DenseMatrix<f64>, Vec<i32>>,
}

impl FraudModel {

    pub fn new() -> Self {

        let x = DenseMatrix::from_2d_vec(&vec![
            vec![1.0, 0.9, 3.0, 6.0],
            vec![0.5, 0.8, 2.0, 5.0],
            vec![10.0, 0.3, 0.0, 1.0],
            vec![8.0, 0.2, 1.0, 2.0],
        ]);

        let y = vec![1, 1, 0, 0];

        let model = LogisticRegression::fit(
            &x,
            &y,
            LogisticRegressionParameters::default(),
        ).unwrap();

        Self { model }
    }

    pub fn predict(&self, features: Vec<f64>) -> f64 {

        let severity = features[1];

        let input = DenseMatrix::from_2d_vec(&vec![features]);

        let result = self.model.predict(&input).unwrap();

        let base = if result[0] == 1 { 0.7 } else { 0.2 };

        let adjustment = (severity * 0.2).min(0.2);

        (base + adjustment).min(1.0)
    }
}